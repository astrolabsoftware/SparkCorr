/*
 * Copyright 2020 AstroLab Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sparkcorr.`2PCF`.Sphere

import org.apache.spark.sql.{functions=>F,SparkSession,DataFrame,Row}
import org.apache.spark.SparkContext
import org.apache.spark.sql.types._
import org.apache.log4j.{Level, Logger}
import org.apache.spark.storage.StorageLevel._

import com.sparkcorr.Binning.{LogBinning}
import com.sparkcorr.IO.{ParamFile}
import com.sparkcorr.Tiling.{SARSPix,HealpixGrid,CubedSphere}
import com.sparkcorr.tools.{Timer}

import scala.math.{log,toRadians,sqrt}

import java.util.Locale
import healpix.essentials.Scheme.{NESTED,RING}



//companion
object PairCount_reduced {
 
 //main
  def main(args:Array[String]):Unit= {
    Locale.setDefault(Locale.US)

    if (args.size!= 2){
      val sep=List.tabulate(25)(i=>"*").reduce(_+_) 
      println(sep)
      println(">>>> Usage: PairCount_reduced paramfile numPart")
      println(sep)
      return
    }

    //Spark initialization
    Logger.getLogger("org").setLevel(Level.WARN)
    Logger.getLogger("akka").setLevel(Level.WARN)

    val spark = SparkSession
      .builder()
      .appName("PairCount_reduced")
      .getOrCreate()
    
    val sc:SparkContext = spark.sparkContext
    val conf =sc.getConf

    conf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    conf.set("spark.kryoserializer.buffer", "1024")
    //conf.registerKryoClasses(Array(classOf[CubedSphere],classOf[SARSPix],classOf[HealpixGrid]))
    //conf.set("spark.broadcast.blockSize","100")


    import spark.implicits._

    //decode parameter file
    val params=new ParamFile(args(0))
    val numPart=args(1).toInt

    //binning
    val Nbins:Int=params.get("Nbins",0)
    val bmin:Double=params.get("bin_min",0.0)
    val bmax:Double=params.get("bin_max",0.0)
    val btype:String=params.get("bin_type","log")


    val fullbin=btype match {
      case "log" => new LogBinning(bmin,bmax,Nbins)
      case _ => throw new Exception("Invalid binning type: "+btype)
    }

   //choose bin range
    require(params.contains("imin") && params.contains("imax"))
    val imin:Int=params.get("imin",0)
    val imax:Int=params.get("imax",0)
    
    val bins:Array[Array[Double]]=fullbin.bins.slice(imin,imax+1)

    val binning=sc.parallelize(bins.zipWithIndex)
      .toDF("interval","bin")
      .withColumn("ibin",$"bin"+imin).drop("bin")
      .withColumn("width",$"interval"(1)-$"interval"(0))
      .select("ibin","interval","width")

    binning.cache.count
    binning.show(truncate=false)

 
    //reading data
    val f1=params.get("data1","")
    val ra_name=params.get("ra_name","ra")
    val dec_name=params.get("dec_name","dec")

    //suppose parquet for the moment
    val df_all=spark.read.parquet(f1)
    println("reading input data="+f1)
    df_all.printSchema
    //read RA,DEC , add id, convert to theta/phi
    val input=df_all.select(ra_name,dec_name)
      .withColumn("theta_s",F.radians(F.lit(90)-F.col(dec_name)))
      .withColumn("phi_s",F.radians(ra_name))
      .drop(ra_name,dec_name)

    val Ndata=input.count()
    println(s"input data size=$Ndata")

   //from here start countning time
   //1 compress data
    val timer=new Timer
    val start=timer.time
 
    //pix
    val tilingR=params.get("tilingR","SARSpix").toLowerCase

    //reducing pixelization
    val width:Array[Double]=fullbin.binW.slice(imin,imax+1)
    val w:Double=width.head

    val rawgridR=tilingR match {
      case "sarspix" => {
        val Nf=SARSPix.pixRadiusLt(w/2)
        new SARSPix(Nf)
      }
      case "cubedsphere" => {
        val Nf=CubedSphere.pixRadiusLt(w/2)
        new CubedSphere(Nf)
      }
      case "healpix" => {
        val nside=HealpixGrid.pixRadiusLt(w/2)
        HealpixGrid(nside, NESTED)
      }

    }

    //broadcast object to executors
    val gridR=sc.broadcast(rawgridR)

    //create cells
    //add index
    val pixmap=input
      .map(r=>gridR.value.ang2pix(r.getDouble(0),r.getDouble(1)))
      .toDF("cellpix")
      .groupBy("cellpix").count()
      .persist(MEMORY_ONLY)

    //actions
    println(pixmap.count)

    val newinput=pixmap
      .map(r=>(gridR.value.pix2ang(r.getInt(0)),r.getLong(1))).toDF("ptg","w")
      .withColumn("theta_s",$"ptg"(0)).withColumn("phi_s",$"ptg"(1)).drop("ptg")
      .persist(MEMORY_ONLY)

    val NpixD=newinput.count
    val tr=timer.step
    timer.print(s"reduced data")

    val tilingJ=params.get("tilingJ","cubedSphere").toLowerCase
    //joininmg pixelization
    val rawgridJ=tilingJ match {
      case "sarspix" => {
        val Nf=SARSPix.pixRadiusGt(bins.last(1)/2)
        new SARSPix(Nf)
      }
      case "cubedsphere" => {
        val Nf=CubedSphere.pixRadiusGt(bins.last(1)/2)
        new CubedSphere(Nf)
      }
      case "healpix" => {
        val nside=HealpixGrid.pixRadiusGt(bins.last(1)/2)
        HealpixGrid(nside, NESTED)
      }

    }
    //broadcast object to executors
    val gridJ=sc.broadcast(rawgridJ)

    println(s"joining pixelization $tilingJ(${rawgridJ.Nbase}): NpixJ=${rawgridJ.Npix}\n")


    //add join index
    val indexedInput=newinput
      .map(r=>(r.getLong(0),gridJ.value.ang2pix(r.getDouble(1),r.getDouble(2)),r.getDouble(1),r.getDouble(2)))
      .toDF("w","ipix","theta_s","phi_s")

    val source=indexedInput
      .withColumn("id",F.monotonicallyIncreasingId)
      .withColumn("x_s",F.sin($"theta_s")*F.cos($"phi_s"))
      .withColumn("y_s",F.sin($"theta_s")*F.sin($"phi_s"))
      .withColumn("z_s",F.cos($"theta_s"))
      .drop("theta_s","phi_s")
      .repartition(numPart,$"ipix")
      .persist(MEMORY_ONLY)

    val np1=source.rdd.getNumPartitions
    println("source #part="+np1)
    source.printSchema

    println("*** caching source: "+source.columns.mkString(", "))
    val Ns=source.count
    println(f"Source size=${Ns/1e6}%3.2f M")
    val tsource=timer.step
    timer.print("input source")
    source.show(5)

    //clean
    pixmap.unpersist()
    newinput.unpersist()

    // 2. duplicates
    //map way
    val neib=source.map(r=>(r.getLong(0),gridJ.value.neighbours(r.getInt(1)),r.getLong(2),r.getDouble(3),r.getDouble(4),r.getDouble(5)))
    .toDF("w","neighbours","id","x_s","y_s","z_s")


  val dfn=neib.withColumn("ipix",F.explode($"neighbours")).drop("neighbours")

    //add input source (with same labels)
    val cols=dfn.columns
    val dup=dfn.union(source.select(cols.head,cols.tail:_*))
      .withColumnRenamed("id","id2")
      .withColumnRenamed("w","w2")
      .withColumnRenamed("x_s","x_t")
      .withColumnRenamed("y_s","y_t")
      .withColumnRenamed("z_s","z_t")
      .repartition(numPart,$"ipix")
      .persist(MEMORY_ONLY)

    println("*** caching duplicates: "+dup.columns.mkString(", "))
    val Ndup=dup.count
    println(f"duplicates size=${Ndup/1e6}%3.2f M")
    dup.show(5)

    val tdup=timer.step
    timer.print("dup cache")
    val np2=dup.rdd.getNumPartitions
    println("dup partitions="+np2)


    // 3. pairs
    val pairs=source.join(dup,"ipix")
      .drop("ipix")
      .filter($"id"<$"id2")
      .drop("id","id2")

    //println("pairs:")
    //pairs.printSchema

    //cuts on cart distance^2 + add (log) bin number
    val rmin:Double =toRadians(bins.head(0)/60.0)
    val rmax:Double =toRadians(bins.last(1)/60.0)
    val r2min:Double =rmin*rmin
    val r2max:Double =rmax*rmax
    //really logged here
    val lrmin:Double =log(rmin)
    val b:Double = (log(bmax)-log(bmin))/Nbins

    val edges=pairs
      .withColumn("dx",$"x_s"-$"x_t").withColumn("dy",$"y_s"-$"y_t").withColumn("dz",$"z_s"-$"z_t")
      .withColumn("r2",$"dx"*$"dx"+$"dy"*$"dy"+$"dz"*$"dz")
      .filter(F.col("r2").between(r2min,r2max))
      .drop("dx","dy","dz","x_t","x_s","y_s","y_t","z_s","z_t")
      .withColumn("logr",F.log($"r2")/2.0)
      .drop("r2")
      .withColumn("bin",(($"logr"-lrmin)/b).cast(IntegerType))
      .drop("logr")
      .withColumn("prod",$"w"*$"w2")
      .drop("w","w2")

    //println("edges:")
    //edges.printSchema
    val np3=edges.rdd.getNumPartitions
    println("edges numParts="+np3)

    println("==> joining "+edges.columns.mkString(", "))


    //count edges? no
    //val nedges=edges.count()
    //println(f"#edges=${nedges/1e9}%3.2f G")
    //val nedges=0.0


    val tjoin=timer.step
    timer.print("join")

    //bin!
    val binned=edges.groupBy("bin").agg(F.sum($"prod") as "Nbin").sort("bin").cache
    //val binned=edges.rdd.map(r=>(r.getInt(0),r.getLong(1))).reduceByKey(_+_).toDF("ibin","Nbin")


    println("#bins="+binned.count)
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

    //cori oriented
    val nodes=System.getenv("SLURM_JOB_NUM_NODES")


    println("Summary: ************************************")
    println("@"+tilingJ+"("+rawgridJ.Nbase+")")
    println("@"+tilingR+"("+rawgridR.Nbase+")")
    println("x@ imin imax Ndata Ndup nedges nbaseD NpixD nbaseJ NpixJ nodes part1 part2 part3 tr ts td tj tb t")
    println(f"r@@ $imin $imax $Ndata $Ndup $nedges%g ${rawgridR.Nbase} ${rawgridR.Npix} ${rawgridJ.Nbase} ${rawgridJ.Npix} $nodes $np1 $np2 $np3 ${tr.toInt} ${tsource.toInt} ${tdup.toInt} ${tjoin.toInt} ${tbin.toInt} $fulltime%.2f")



    //nice output+sum
    binning.join(binned.withColumn("ibin",$"bin"+imin).drop("bin"),"ibin").show(Nbins,truncate=false)
    sumbins.show

    spark.close





  }

}

