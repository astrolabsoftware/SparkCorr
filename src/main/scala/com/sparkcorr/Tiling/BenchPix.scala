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

    if (args.size!=3){
      println("**********************************************")
      println(">>>> Usage: BenchPix cs/hp N Ngen")
      println("**********************************************")
      return
    }

    //parameters
    val tile=args(0)
    val Nf:Int=args(1).toInt
    val N:Long=args(2).toLong

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
    conf.set("spark.kryoserializer.buffer", "4096")
    //conf.set("spark.kryo.registrationRequired", "true")
    conf.registerKryoClasses(Array(classOf[CubedSphere],classOf[SARSPix],classOf[HealpixGrid]))


    import spark.implicits._

    val timer=new Timer()

    val grid = tile match {
      case "cs" => new CubedSphere(Nf)
      case "sa" => new SARSPix(Nf)
      case "hp" => HealpixGrid(Nf, NESTED)
      case _ =>  {require(false,s"unknow tiling=$tile"); null}
    }

    //val grid=new CubedSphere(Nf)
    //sc.broadcast(grid.pixcenter)

    //as array
    //val pixc:Array[ Array[Double] ]=grid.pixcenter.map{ case (t,f) => Array(t,f) case null=>null }
    //val pixc:Array[(Double,Double)]=grid.pixcenter
    //sc.broadcast(pixc)

    timer.step
    timer.print("grid creation")

    def Ang2Pix=spark.udf.register("Ang2Pix",(theta:Double,phi:Double)=>grid.ang2pix(theta,phi))
    val Pix2Ang=spark.udf.register("Pix2Ang",(ipix:Int)=> grid.pix2ang(ipix))
    //val Pix2Ang=spark.udf.register("Pix2Ang",(ipix:Int)=> {val p=pixc(ipix); Array(p._1,p._2)})


    var df=spark.range(0,N)
      .withColumn("theta",F.acos(F.rand*2-1.0))
      .withColumn("phi",F.rand*2*Pi).drop("id")

    //add pixelnum
    df=df.withColumn("ipix",Ang2Pix($"theta",$"phi"))

    df.select(F.min($"ipix"),F.max($"ipix")).show()
    timer.step
    timer.print(s"ang2pix")

    //add pixel center
    df=df.withColumn("ang",Pix2Ang($"ipix"))
    df=df.withColumn("theta_c",$"ang"(0)).withColumn("phi_c",$"ang"(1)).drop("ang")

    df.select(F.min($"theta_c"),F.min($"phi_c")).show()

    timer.step
    timer.print("pix2ang")

    df=df.persist(MEMORY_ONLY)

    def Neighbours=spark.udf.register("pix_neighbours",(ipix:Int)=>grid.neighbours(ipix))

    val dup=df.withColumn("neigbours",Neighbours($"ipix"))
      .drop("ipix").withColumn("ipix",F.explode($"neigbours")).drop("neighbours")

    dup.select(F.min($"ipix"),F.max($"ipix")).show()
    timer.step
    timer.print(s"neighbours")


  }

}

