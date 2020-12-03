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

package com.sparkcorr.Tiling

import org.apache.spark.sql.{functions=>F,SparkSession}
import org.apache.spark.SparkContext

import org.apache.log4j.{Level, Logger}
import java.util.Locale

import com.sparkcorr.Geometry.{Point,Point3D,arr2}
import com.sparkcorr.tools.Timer

import scala.math.{Pi,sqrt,toRadians,ceil,floor,toDegrees}

import scala.util.Random

import healpix.essentials.Scheme.{NESTED,RING}

import org.apache.spark.storage.StorageLevel._

object BenchPix {

  def main(args:Array[String]):Unit= {

    if (args.size!=4){
      println("**********************************************")
      println(">>>> Usage: BenchPix cs/sa/hp nbase Ngen numpart")
      println("**********************************************")
      return
    }

    //parameters
    val tile=args(0)
    val Nf:Int=args(1).toInt
    val N:Long=args(2).toLong
    val numpart:Int=args(3).toInt

    Locale.setDefault(Locale.US)

   // Set verbosity
    Logger.getLogger("org").setLevel(Level.WARN)
    Logger.getLogger("akka").setLevel(Level.WARN)


    //spark stuff
    val spark = SparkSession
      .builder()
      .appName("Bench"+tile)
      .getOrCreate()


    val sc=spark.sparkContext
    val conf =sc.getConf
    conf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    conf.set("spark.kryoserializer.buffer", "1024")

    import spark.implicits._

    val griddriver = tile match {
      case "cs" => new CubedSphere(Nf)
      case "sa" => new SARSPix(Nf)
      case "hp" => HealpixGrid(Nf, NESTED)
      case _ =>  {require(false,s"unknow tiling=$tile"); null}
    }
    val grid=sc.broadcast(griddriver)

    val df=spark.range(0,N,1,numPartitions=numpart)
      .withColumn("theta",F.acos(F.rand*2-1.0))
      .withColumn("phi",F.rand*2*Pi).drop("id")
    .cache

    println(df.count)

    val timer=new Timer()
    val startime=timer.step

    //add pixelnum
   val df1=df.map(r=> grid.value.ang2pix(r.getDouble(0),r.getDouble(1))).toDF("ipix").cache

    df.unpersist

    df1.select(F.min($"ipix"),F.max($"ipix")).show()
    val t1=timer.step
    timer.print(s"ang2pix")

    //add pixel center
    val df2=df1.map(r=>grid.value.pix2ang(r.getInt(0))).toDF("ang")
      .withColumn("theta_c",$"ang"(0)).withColumn("phi_c",$"ang"(1)).drop("ang")

    df2.select(F.min($"theta_c"),F.min($"phi_c")).show()

    val t2=timer.step
    timer.print("pix2ang")

    val df3=df1.map(r=>grid.value.neighbours(r.getInt(0))).toDF("neib")

    df3.select(F.size($"neib").as("size")).select(F.min($"size")).show

    val t3=timer.step
    timer.print(s"neighbours")

    val tot=t1+t2+t3
    println(s"TOT time for $tile: $tot s - ${tot/60} mins")



  }

}

