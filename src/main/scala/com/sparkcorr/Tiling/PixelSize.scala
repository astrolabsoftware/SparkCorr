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
import org.apache.log4j.{Level, Logger}
import java.util.Locale
import scala.math.{Pi}

import healpix.essentials.Scheme.{NESTED,RING}

import com.sparkcorr.tools.Timer

object PixelSize {


  def main(args:Array[String]):Unit= {

    if (args.size!=3){
      println("**********************************************")
      println(">>>> Usage: PixelSize sa/cs/hp N(side) Ngen")
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
      .appName("CubedSphereSize")
      .getOrCreate()

    import spark.implicits._

    val grid = tile match {
      case "cs" => new CubedSphere(Nf)
      case "sa" => new SARSPix(Nf)
      case "hp" => HealpixGrid(Nf, NESTED)
      case _ =>  {require(false,s"unknow tiling=$tile"); null}
    }

    def Ang2Pix=spark.udf.register("Ang2Pix",(theta:Double,phi:Double)=>grid.ang2pix(theta,phi))
    val Pix2Ang=spark.udf.register("Pix2Ang",(ipix:Int)=> grid.pix2ang(ipix))


    var df=spark.range(0,N).withColumn("theta",F.acos(F.rand*2-1.0)).withColumn("phi",F.rand*2*Pi).drop("id")

    val timer=new Timer()

    //add pixelnum
    df=df.withColumn("ipix",Ang2Pix($"theta",$"phi"))

    //add pixel center
    df=df.withColumn("ang",Pix2Ang($"ipix"))

    df=df.withColumn("theta_c",$"ang"(0)).withColumn("phi_c",$"ang"(1)).drop("ang")
    println(df.count)
    timer.print(tile+" Ang2Pix+Pix2Ang")

    val file=tile+s"_nside${Nf}.parquet"
    println("Writing "+file)
    df.write.mode("overwrite").parquet(file)


  }

}

