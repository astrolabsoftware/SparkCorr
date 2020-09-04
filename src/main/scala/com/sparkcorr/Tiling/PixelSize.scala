
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

import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.{functions=>F}
import org.apache.spark.sql.types._
import org.apache.log4j.{Level, Logger}
import org.apache.spark.SparkContext
import org.apache.spark.sql.{functions=>F,SparkSession,Row,DataFrame}
import java.util.Locale
import scala.math.{Pi}


import healpix.essentials.HealpixBase
import healpix.essentials.Pointing
import healpix.essentials.Vec3
import healpix.essentials.Scheme.{NESTED,RING}

import com.sparkcorr.tools.Timer

class ExtPointing extends Pointing with java.io.Serializable
case class HealpixGrid(hp : HealpixBase, ptg : ExtPointing) {

  def index(theta : Double, phi : Double) : Long = {
    ptg.theta = theta
    ptg.phi = phi
    hp.ang2pix(ptg)
  }
  def neighbours(ipix:Long):Array[Long] =  {
    hp.neighbours(ipix)
  }
  def pix2ang(ipix:Long):Array[Double]=
  {
    val p:Pointing=hp.pix2ang(ipix)
    Array(p.theta,p.phi)
  }
  def pix2vec(ipix:Long):Array[Double]= {
    val pos:Vec3=hp.pix2vec(ipix)
    Array(pos.x,pos.y,pos.z)
  }

}

object HealpixSize {


  def main(args:Array[String]):Unit= {

    require(args.size==2)

    //parameters
    val nside:Int=args(0).toInt
    val N:Long=args(1).toLong

    Locale.setDefault(Locale.US)

   // Set verbosity
    Logger.getLogger("org").setLevel(Level.WARN)
    Logger.getLogger("akka").setLevel(Level.WARN)


    //spark stuff
    val spark = SparkSession
      .builder()
      .appName("HealpixSize")
      .getOrCreate()

    val sc: SparkContext = spark.sparkContext

    import spark.implicits._

    val grid = HealpixGrid(new HealpixBase(nside, NESTED), new ExtPointing)
    def Ang2Pix=spark.udf.register("Ang2Pix",(theta:Double,phi:Double)=>grid.index(theta,phi))
    val Pix2Ang=spark.udf.register("Pix2Ang",(ipix:Long)=> grid.pix2ang(ipix))

    val timer=new Timer()

    var df=spark.range(0,N).withColumn("theta",F.acos(F.rand*2-1.0)).withColumn("phi",F.rand*2*Pi).drop("id")

    //add pixelnum
    df=df.withColumn("ipix",Ang2Pix($"theta",$"phi"))

    //add pixel center
    df=df.withColumn("ptg",Pix2Ang($"ipix")).withColumn("theta_c",$"ptg"(0)).withColumn("phi_c",$"ptg"(1)).drop("ptg")

    println(df.count)
    timer.print("Healpix Ang2Pix+Pix2Ang")

    df.write.mode("overwrite").parquet(s"hp_nside${nside}.parquet")

  }

}

object CubedSphereSize {


  def main(args:Array[String]):Unit= {

    require(args.size==2)

    //parameters
    val Nf:Int=args(0).toInt
    val N:Long=args(1).toLong

    Locale.setDefault(Locale.US)

   // Set verbosity
    Logger.getLogger("org").setLevel(Level.WARN)
    Logger.getLogger("akka").setLevel(Level.WARN)


    //spark stuff
    val spark = SparkSession
      .builder()
      .appName("CubedSphereSize")
      .getOrCreate()

    val sc: SparkContext = spark.sparkContext

    import spark.implicits._

    val grid = new CubedSphere(Nf)
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
    timer.print("Cubedsphere Ang2Pix+Pix2Ang")

    df.write.mode("overwrite").parquet(s"cs_nside${Nf}.parquet")


  }

}

