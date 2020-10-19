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

import com.sparkcorr.Binning.{LogBinning}
import com.sparkcorr.IO.{ParamFile}
import com.sparkcorr.Tiling.{SARSPix}
import com.sparkcorr.tools.{Timer}

import scala.math.{log,toRadians}

import java.util.Locale





//companion
object PairCount_exact {
 
 //main
  def main(args:Array[String]):Unit= {
    Locale.setDefault(Locale.US)

    if (args.size!= 1){
      val sep=List.tabulate(25)(i=>"*").reduce(_+_) 
      println(sep)
      println(">>>> Usage: PairCount_exact paramfile")
      println(sep)
      return
    }

    //Spark initialization
    Logger.getLogger("org").setLevel(Level.WARN)
    Logger.getLogger("akka").setLevel(Level.WARN)

    val spark = SparkSession
      .builder()
      .appName("PairCount_exact")
      .getOrCreate()
    
    val sc:SparkContext = spark.sparkContext
    
    import spark.implicits._

    //decode parameter file
    val params=new ParamFile(args(0))

    //binning
    val Nbins:Int=params.get("Nbins",0)
    val bmin:Double=params.get("bin_min",0.0)
    val bmax:Double=params.get("bin_max",0.0)
    val btype:String=params.get("bin_type","log")


    val fullbin=btype match {
      case "log" => new LogBinning(bmin,bmax,Nbins)
      case _ => throw new Exception("Invalid binning type: "+btype)
    }


    //reading data
    val f1=params.get("data1","")

    val ra_name=params.get("ra_name","ra")
    val dec_name=params.get("dec_name","dec")


    //suppose parquet for the moment
    val df_all=spark.read.parquet(f1)
    println("reading input data="+f1)
    df_all.printSchema
    
    val input=df_all.select(ra_name,dec_name)

    val N1=input.count()


    //find automatically imax

    //choose range
    require(params.contains("imin") && params.contains("imax"))
    val imin:Int=params.get("imin",0)
    val imax:Int=params.get("imax",0)
    
    val bins=fullbin.bins.slice(imin,imax+2)


    val binning=sc.parallelize(bins.zipWithIndex)
      .toDF("interval","ibin")
      .withColumn("width",$"interval"(1)-$"interval"(0))
      .select("ibin","interval","width")

    binning.cache.count
    binning.show(truncate=false)


    //joining pixelization
    val til=params.get("tiling","SARSPix").toLowerCase
    val Nf=SARSPix.pixRadiusGt(bins.last(1)/2)
    val Npix=SARSPix.Npix(Nf)
    println(s"Use SARSPIx as joining pixelization Nf=$Nf NpixJ=$Npix")

    val grid=new SARSPix(Nf)
    def Ang2Pix=spark.udf.register("Ang2Pix",(theta:Double,phi:Double)=>grid.ang2pix(theta,phi))

    val timer=new Timer
    val start=timer.time

    //and index and replace by cartesian coords
    var source=input
      .withColumn("id",F.monotonicallyIncreasingId)
      .withColumn("theta_s",F.radians(F.lit(90)-F.col(dec_name)))
      .withColumn("phi_s",F.radians(ra_name))
      .withColumn("ipix",Ang2Pix($"theta_s",$"phi_s"))
      .withColumn("x_s",F.sin($"theta_s")*F.cos($"phi_s"))
      .withColumn("y_s",F.sin($"theta_s")*F.sin($"phi_s"))
      .withColumn("z_s",F.cos($"theta_s"))
      .drop(ra_name,dec_name,"theta_s","phi_s")

    //optional repartionning
    val numPart=params.get[Int]("numPart",_.toInt)

    numPart match {
      case Some(np)=> source=source.repartition(np,$"ipix")
      case None=> println("---> no repartitioning specified")
    }

    source=source.cache
    

    val np1=source.rdd.getNumPartitions
    println("source #part="+np1)

    println("*** caching source: "+source.columns.mkString(", "))
    val Ns=source.count
    println(f"Source size=${Ns/1e6}%3.2f M")
    val tsource=timer.step
    source.show(5)


    // 2. duplicates
    def Neighbours=spark.udf.register("pix_neighbours",(ipix:Int)=>grid.neighbours(ipix))

    val dfn=source.withColumn("neighbours",Neighbours($"ipix"))

    val dups=new Array[org.apache.spark.sql.DataFrame](9)

    for (i <- 0 to 7) {
      println(i)
      val df1=dfn.drop("ipix").withColumn("ipix",$"neighbours"(i))
      val dfclean1=df1.filter(F.not(df1("ipix")===F.lit(-1))).drop("neighbours")
      dups(i)=dfclean1
    }
    val cols=dups(0).columns
    dups(8)=source.select(cols.head,cols.tail:_*)

    var dup=dups.reduceLeft(_.union(_))
      .withColumnRenamed("id","id2")
      .withColumnRenamed("x_s","x_t")
      .withColumnRenamed("y_s","y_t")
      .withColumnRenamed("z_s","z_t")


    numPart match {
      case Some(np)=> dup=dup.repartition(np,$"ipix")
      case None=> println("---> no repartitioning specified")
    }

    dup=dup.cache


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

    println("pairs:")
    pairs.printSchema

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
      .withColumn("ibin",(($"logr"-lrmin)/b).cast(IntegerType))
      .drop("logr")
    //  .persist(StorageLevel.MEMORY_AND_DISK)

    println("edges:")
    edges.printSchema
    val np3=edges.rdd.getNumPartitions
    println("edges numParts="+np3)

    println("==> joining with Nf="+Nf+" output="+edges.columns.mkString(", "))


    //count edges? no
    //val nedges=edges.count()
    //println(f"#edges=${nedges/1e9}%3.2f G")
    //val nedges=0.0


    val tjoin=timer.step
    timer.print("join")

    //bin!
    val binned=edges.groupBy("ibin").count.withColumnRenamed("count","Nbin").sort("ibin").cache
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
    val nodes=params.get("nodes",-1)


    println("Summary: ************************************")
    println("x@ imin imax Ndata nedges nodes part1 part2 part3 ts td tj tb t")
    println(s"x@@$imin $imax $nedges%g $nodes $np1 $np2 $np3 ${tsource.toInt} ${tdup.toInt} ${tjoin.toInt} {tbin.toInt} $fulltime%.2f")



    //nice output+sum
    binning.join(binned,"ibin").show(Nbins,truncate=false)
    sumbins.show

    spark.close





  }

}
