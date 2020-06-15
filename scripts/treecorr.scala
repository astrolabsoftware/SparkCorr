//spark-shell $SPARKOPTS --jars $JARS -I hpgrid.scala -I Timer.scala

import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.{functions=>F}
import org.apache.spark.sql.Row
import org.apache.spark.storage.StorageLevel
import org.apache.spark.sql.types._

import java.util.Locale
import scala.math.{sin,toRadians,log,pow,floor,exp}

Locale.setDefault(Locale.US)

//args from --conf spark.driver.args="sepcut zmax numpart"
//val args = sc.getConf.get("spark.driver.args").split("\\s+")

//logarithmic binning in arcmin
val Nbins:Int=20

val b_arcmin:Double = (log(250)-log(2.5))/Nbins
val b=toRadians(b_arcmin/60.0)

//en arcmin
val logt=List.tabulate(Nbins+1)(i=>log(2.5)+i*b_arcmin)
val t=logt.map(exp)
val bins=t.sliding(2).toList
val tmin=t.head
val tmax=t.last

val binning=sc.parallelize(bins.zipWithIndex).toDF("interval","ibin").withColumn("width",$"interval"(1)-$"interval"(0)).select("ibin","interval","width")
binning.cache.count
binning.show(truncate=false)

//in rads
val r=t.map(x=>toRadians(x/60))

val rmin=r.head
val rmax=r.last
val r2min=rmin*rmin
val r2max=rmax*rmax

//input////////////////
//input in theta phi

val timer=new Timer
val start=timer.time


import spark.implicits._

val input=spark.read.parquet(System.getenv("INPUT")).drop("ipix","z")
  .withColumn("theta",F.radians(F.lit(90)-F.col("DEC")))
  .withColumn("phi",F.radians("RA"))
  .drop("RA","DEC")

input.cache.count

//add multiresol indices
val nsides=List(32,64,128,256)
val s=nsides.map(sqrt(Pi/3)/_)


var multi=input
for (nside1 <- nsides) {
  val grid1 = HealpixGrid(new HealpixBase(nside1, NESTED), new ExtPointing)
  val Ang2Pix1=spark.udf.register("Ang2Pix1",(theta:Double,phi:Double)=>grid1.index(theta,phi))
 multi=multi.withColumn(s"id$nside1",Ang2Pix1($"theta",$"phi"))
}

multi.cache.count

//construct cell at given resolution (colpix)
def makeCell(df:DataFrame,nside1:Int,colpix:String):DataFrame ={

  println("making cell  with nside="+nside1)
  val pixmap=df.groupBy(F.col(colpix)).count().withColumnRenamed("count","N")

  //add pixel centers
  val grid1 = HealpixGrid(new HealpixBase(nside1, NESTED), new ExtPointing)

  /*
  val Pix2Ang1=spark.udf.register("Pix2Ang1",(ipix:Long)=> grid1.pix2ang(ipix))
  pixmap.withColumn("ptg",Pix2Ang1(F.col(colpix)))
    .withColumn("theta1",$"ptg"(0))
    .withColumn("phi1",$"ptg"(1))
    .drop("ptg")
    .withColumn(s"x$nside1",F.sin($"theta1")*F.cos($"phi1"))
    .withColumn(s"y$nside1",F.sin($"theta1")*F.sin($"phi1"))
    .withColumn(s"z$nside1",F.cos($"theta1"))
    .drop("theta1","phi1")
   */

  //verifier que ca marche
  val Pix2Vec1=spark.udf.register("Pix2Vec1",(ipix:Long)=> grid1.pix2ang(ipix))

  pixmap.withColumn("pos",Pix2Vec1(F.col(colpix)))
    .withColumn(s"x$nside1",$"pos"(0))
    .withColumn(s"y$nside1",$"pos"(1))
    .withColumn(s"z$nside1",$"pos"(2))
    .drop("pos")

}

/////////////////////////////////////////
//1 reduce data

//ou direct
/*
val input1=input
  .withColumn("theta",F.radians(F.lit(90)-F.col("DEC")))
  .withColumn("phi",F.radians("RA"))
  .withColumn("cell1",Ang2Pix1($"theta",$"phi"))
  .drop("RA","DEC")
  .withColumn("pos",Pix2Vec1($"cell1"))
  .withColumn("x1",$"pos"(0))
  .withColumn("y1",$"pos"(1))
  .withColumn("z1",$"pos"(2))
  .drop("theta","phi")
  .cache
*/

val Nin=input1.count
println(f"input size=${Nin/1e6}%3.2f M")

//reduce to wighted pixmap. id is bigpix number
val pixmap=input1.groupBy("cell1").count()

//postions are pixel centers
val newinput=pixmap.withColumnRenamed("count","w")
  .withColumn("ptg",Pix2Ang1($"cell1"))
  .select($"bigpix",$"ptg"(0) as "theta_s",$"ptg"(1) as "phi_s",$"w")


//println("Reduced data size="+newinput.count)
// automatic
val i=floor(-log(rmax)/log(2.0)).toInt
val nside2=pow(2,i).toInt

println(s"$tmax arcmin -> nside=$nside2")


val grid = HealpixGrid(new HealpixBase(nside2, NESTED), new ExtPointing)
def Ang2pix=spark.udf.register("Ang2pix",(theta:Double,phi:Double)=>grid.index(theta,phi))
def pix_neighbours=spark.udf.register("pix_neighbours",(ipix:Long)=>grid.neighbours(ipix))


val source=newinput
  //.withColumn("id",F.monotonicallyIncreasingId)
  .withColumn("ipix",Ang2pix($"theta_s",$"phi_s"))
  .withColumn("x_s",F.sin($"theta_s")*F.cos($"phi_s"))
  .withColumn("y_s",F.sin($"theta_s")*F.sin($"phi_s"))
  .withColumn("z_s",F.cos($"theta_s"))
  .drop("theta_s","phi_s")
//  .repartition(numPart,$"ipix")
  .cache()


println("*** caching source: "+source.columns.mkString(", "))
val Ns=source.count
println(f"reduced size=${Ns/1e6}%3.2f M")
val tsource=timer.step
timer.print("source cache")

val np1=source.rdd.getNumPartitions
println("source partitions="+np1)
source.show(5)
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
  .withColumnRenamed("w","w2")
  .withColumnRenamed("bigpix","bigpix2")
  .withColumnRenamed("x_s","x_t")
  .withColumnRenamed("y_s","y_t")
  .withColumnRenamed("z_s","z_t")
  .cache

println("*** caching duplicates: "+dup.columns.mkString(", "))
val Ndup=dup.count
println(f"duplicates size=${Ndup/1e6}%3.2f M")

val tdup=timer.step
timer.print("dup cache")
val np2=dup.rdd.getNumPartitions
println("dup partitions="+np2)
dup.show(5)
///////////////////////////////////////////
//3 build PAIRS with cuts
/*
val pairs=source.join(dup,"ipix")
  .drop("ipix")
  .filter($"bigpix"<"bigpix2")
  //.drop("bigpix2")
 */
val pairs=source.join(dup,"ipix")
	.drop("ipix")
	.filter($"bigpix"<$"bigpix2")
	.drop("bigpix","bigpix2")

println("pairs:")
pairs.printSchema

//println("#pairs="+pairs.count)



//cut on cart distance+bin
val edges=pairs
  .withColumn("dxc",$"x_s"-$"x_t")
  .withColumn("dyc",$"y_s"-$"y_t")
  .withColumn("dzc",$"z_s"-$"z_t")
  .withColumn("r2",$"dxc"*$"dxc"+$"dyc"*$"dyc"+$"dzc"*$"dzc")
  .filter(F.col("r2").between(r2min,r2max))
  .drop("dxc","dyc","dzc","x_t","x_s","y_s","y_t","z_s","z_t")
  .withColumn("r",F.sqrt($"r2"))
  .withColumn("ibin",(($"r"-rmin)/dr).cast(IntegerType))
  .drop("r2","r")
  .withColumn("prod",$"w"*$"w2")
  .drop("w","w2")
//  .persist(StorageLevel.MEMORY_AND_DISK)


println("edges:")
edges.printSchema


val np3=edges.rdd.getNumPartitions
println("edges #partitions="+np3)

println("==> joining with nside2="+nside2+": output="+edges.columns.mkString(", "))
//val nedges=edges.count()
//val nedges=0.0
//println(f"#edges=${nedges/1e9}%3.2f G")

val tjoin=timer.step
timer.print("join")

///////////////////////////////////
//4 binning

val binned=edges.groupBy("ibin").agg(F.sum($"prod") as "Nbin").sort("ibin").cache

//val binned=edges.rdd.map(r=>(r.getInt(0),r.getLong(1))).reduceByKey(_+_).toDF("ibin","Nbin")
//nice show

//nedges
val sumbins=binned.agg(F.sum($"Nbin"))
val nedges=sumbins.take(1)(0).getLong(0)

val tbin=timer.step
timer.print("binning")


val fulltime=(timer.time-start)*1e-9/60
println(s"TOT TIME=${fulltime} mins")

val nodes=System.getenv("SLURM_JOB_NUM_NODES")

println("\nSummary: ************************************")
println("@| tmin | tmax | Nbins | bW | nside1 | nside2 | Ns |  Ne  | time")
println(f"@| $tmin | $tmax | $Nbins | $binSize | $nside1 | $nside2 | $Ns%g | $nedges%g | $fulltime%.2f")
println(f"@ nodes=$nodes parts=($np1 | $np2 | $np3): source=${tsource.toInt}s dups=${tdup.toInt}s join=${tjoin.toInt}s bins=${tbin.toInt} |  tot=$fulltime%.2f mins")

//nice output+sum
binning.join(binned,"ibin").show(Nbins)
sumbins.show




System.exit(0)
