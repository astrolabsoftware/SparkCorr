//spark-shell --jars jhealpix.jar -I hpgrid.scala -I Timer.scala

import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.{functions=>F}
import org.apache.spark.sql.Row
import org.apache.spark.storage.StorageLevel
import org.apache.spark.sql.types._

import java.util.Locale
import scala.math.{sin,toRadians,log,exp,pow,floor}

// spark3D implicits
import com.astrolabsoftware.spark3d._

Locale.setDefault(Locale.US)

//args from --conf spark.driver.args="sepcut zmax numpart"
val args = sc.getConf.get("spark.driver.args").split("\\s+")

val imin=args(0).toInt
val imax=args(1).toInt
val nside1=args(2).toInt
val numPart=args(3).toInt

//constant log binning
val Nbins=20
val b_arcmin:Double = (log(250)-log(2.5))/Nbins
val b=b_arcmin

println("logbw="+b)


//en arcmin
val logt=List.tabulate(Nbins+1)(i=>log(2.5)+i*b_arcmin).slice(imin,imax+2)
val t=logt.map(exp)
val bins=t.sliding(2).toList
val tmin=t.head
val tmax=t.last

val binning=sc.parallelize(bins.zipWithIndex).toDF("interval","ibin").withColumn("width",$"interval"(1)-$"interval"(0)).select("ibin","interval","width")
binning.cache.count
binning.show(truncate=false)

//extrait!
val rmin=toRadians(bins.head(0)/60.0)
val rmax=toRadians(bins.last(1)/60.0)
val r2min=rmin*rmin
val r2max=rmax*rmax


val timer=new Timer
val start=timer.time

import spark.implicits._

//input
val input=spark.read.parquet(System.getenv("INPUT")).drop("ipix","z")

//println("Input data size="+input.count+" part="+input.rdd.getNumPartitions)

//1-data reduction
println("reducing data with nside1="+nside1)

val grid1 = HealpixGrid(new HealpixBase(nside1, NESTED), new ExtPointing)
val Ang2Pix1=spark.udf.register("Ang2Pix1",(theta:Double,phi:Double)=>grid1.index(theta,phi))
val Pix2Ang1=spark.udf.register("Pix2Ang1",(ipix:Long)=> grid1.pix2ang(ipix))


//create cell
val pixmap=input
  .withColumn("theta",F.radians(F.lit(90)-F.col("DEC")))
  .withColumn("phi",F.radians("RA"))
  .withColumn("cellpix",Ang2Pix1($"theta",$"phi"))
  .drop("RA","DEC","theta","phi")
  .groupBy("cellpix").count()
  //.repartition(numPart)

//add pixel centers
val newinput=pixmap
  .withColumn("ptg",Pix2Ang1($"cellpix"))
  .withColumn("theta_s",$"ptg"(0))
  .withColumn("phi_s",$"ptg"(1))
  .withColumnRenamed("count","w")
  .drop("ptg")

val Nin=newinput.cache.count
println(f"new size=${Nin/1e6}%3.2f M part="+newinput.rdd.getNumPartitions)
val tred=timer.step
timer.print("reduction")

// 2- JOIN depending on rmax
val i=floor(-log(rmax)/log(2.0)).toInt
val NSIDE=pow(2,i).toInt
//val NSIDE=pow(2,i-1).toInt
println(s"$tmax arcmin -> nside=$NSIDE")

val grid = HealpixGrid(new HealpixBase(NSIDE, NESTED), new ExtPointing)
def Ang2pix=spark.udf.register("Ang2pix",(theta:Double,phi:Double)=>grid.index(theta,phi))
def pix_neighbours=spark.udf.register("pix_neighbours",(ipix:Long)=>grid.neighbours(ipix))

//
val source=newinput
  .withColumnRenamed("cellpix","id")
  .withColumn("ipix",Ang2pix($"theta_s",$"phi_s"))
  .withColumn("x_s",F.sin($"theta_s")*F.cos($"phi_s"))
  .withColumn("y_s",F.sin($"theta_s")*F.sin($"phi_s"))
  .withColumn("z_s",F.cos($"theta_s"))
  .drop("theta_s","phi_s")
  .repartition(numPart,$"ipix")
  .cache()


val np1=source.rdd.getNumPartitions
println("source #part="+np1)

println("*** caching source: "+source.columns.mkString(", "))
val Ns=source.count
println(f"Source size=${Ns/1e6}%3.2f M")
val tsource=timer.step

source.show(5)

newinput.unpersist()

//////////////////////////////////////////////
//2 build duplicates
val dfn=source.withColumn("neighbours",pix_neighbours($"ipix"))

val dups=new Array[org.apache.spark.sql.DataFrame](9)

for (i <- 0 to 7) {
  println(i)
  val df1=dfn.drop("ipix").withColumn("ipix",$"neighbours"(i))
  val dfclean1=df1.filter(not(df1("ipix")===F.lit(-1))).drop("neighbours")
  dups(i)=dfclean1
}
val cols=dups(0).columns
dups(8)=source.select(cols.head,cols.tail:_*)

val dup=dups.reduceLeft(_.union(_))
  .withColumnRenamed("id","id2")
  .withColumnRenamed("x_s","x_t")
  .withColumnRenamed("y_s","y_t")
  .withColumnRenamed("z_s","z_t")
  .withColumnRenamed("w","w2")
  .cache

println("*** caching duplicates: "+dup.columns.mkString(", "))
val Ndup=dup.count
println(f"duplicates size=${Ndup/1e6}%3.2f M")
dup.show(5)

val tdup=timer.step
timer.print("dup cache")
val np2=dup.rdd.getNumPartitions
println("dup partitions="+np2)

///////////////////////////////////////////
//3 build PAIRS with cuts
val pairs=source.join(dup,"ipix")
  .drop("ipix")
  .filter($"id"<$"id2")
  .drop("id","id2")

println("pairs:")
pairs.printSchema

//cut on cart distance+bin
val lrmin=log(rmin)

val edges=pairs
  .withColumn("dx",$"x_s"-$"x_t")
  .withColumn("dy",$"y_s"-$"y_t")
  .withColumn("dz",$"z_s"-$"z_t")
  .withColumn("r2",$"dx"*$"dx"+$"dy"*$"dy"+$"dz"*$"dz")
  .filter(F.col("r2").between(r2min,r2max))
  .drop("dx","dy","dz","x_t","x_s","y_s","y_t","z_s","z_t")
  .withColumn("logr",F.log($"r2")/2.0)
  .drop("r2")
  .withColumn("ibin",(($"logr"-lrmin)/b).cast(IntegerType))
  .drop("logr")
  .withColumn("prod",$"w"*$"w2")
  .drop("w","w2")
//  .persist(StorageLevel.MEMORY_AND_DISK)

println("edges:")
val np3=edges.rdd.getNumPartitions
println("edges #part="+np3)
edges.printSchema

println("==> joining with NSIDE="+NSIDE+" output="+edges.columns.mkString(", "))

//val nedges=edges.count()
//println(f"#edges=${nedges/1e9}%3.2f G")
//val nedges=0.0


val tjoin=timer.step
timer.print("join")

///////////////////////////////////
//4 binning
val binned=edges.groupBy("ibin").agg(F.sum($"prod") as "Nbin").sort("ibin").cache

//val binned=edges.rdd.map(r=>(r.getInt(0),r.getLong(1))).reduceByKey(_+_).toDF("ibin","Nbin")


//println("#bins="+binned.count)
binned.show(Nbins)

//nedges
val sumbins=binned.agg(F.sum($"Nbin"))
val nedges=sumbins.take(1)(0).getLong(0)

val tbin=timer.step
timer.print("binning")


/*
//degree
val deg=edges.groupBy("id").count
dup.unpersist

println("waiting for deg...")
println(deg.cache.count)
//stats
deg.describe("count").show()

val tdeg=timer.step
timer.print("degree")
 */

val fulltime=(timer.time-start)*1e-9/60
println(s"TOT TIME=${fulltime} mins")


val nodes=System.getenv("SLURM_JOB_NUM_NODES")

println("Summary: ************************************")
println("@| t0 | t1 | Nbins | log(bW) | nside1 | nsideJ | Ns |  Ne  | time")
println(f"@| $tmin%.2f | $tmax%.2f | ${imax-imin+1} | $b_arcmin%g | $nside1 | $NSIDE | $Ns%g | $nedges%g | $fulltime%.2f")
println(f"@ nodes=$nodes parts=($np1 | $np2 | $np3): red=${tred.toInt}s source=${tsource.toInt}s dups=${tdup.toInt}s join=${tjoin.toInt}s bins=${tbin.toInt} |  tot=$fulltime%.2f mins")


//nice output+sum
binning.join(binned,"ibin").show(Nbins,truncate=false)
sumbins.show

spark.close

System.exit(0)
