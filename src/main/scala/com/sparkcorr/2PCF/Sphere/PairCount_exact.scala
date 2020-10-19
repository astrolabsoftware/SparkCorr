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

import org.apache.log4j.{Level, Logger}

import com.sparkcorr.Binning.{LogBinning}
import com.sparkcorr.IO.{ParamFile}
import com.sparkcorr.Tiling.{SARSPix}
import com.sparkcorr.tools.{Timer}

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
    val til=params.get("tiling","SARSPix").toLowerCase

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
    val tile=params.get("tiling","sarspix").toLowerCase
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







  }

}

