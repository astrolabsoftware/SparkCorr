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

import com.sparkcorr.Geometry.{Point,Point3D,arr2}

import scala.math.{Pi,sqrt,toRadians,ceil,floor,toDegrees}

import org.apache.log4j.{Level, Logger}

import java.io._
import java.util.Locale




class SARS(nside:Int) extends CubedSphere(nside) {


  require(nside%2==0,"nside for SARS must be even")

  //Nodes construction
  override  def buildNodes():Array[arr2[Point]]={

    val nodes=new Array[arr2[Point]](6)


    nodes
  }

  override def ang2pix(theta:Double,phi:Double):Int = {
    val face:Int=getFace(theta,phi)

    val p=new Point3D(theta,phi)
    //subsface
    // subface index i,j

    //back to face index
    val (ii,jj)=(0,0)
    coord2pix(face,ii,jj)
  }




}



// companion
object SARS {


  def main(args:Array[String]):Unit= {

    if (args.size!=1){
      println("*****************************************")
      println(">>>> Usage: SARS nside")
      println("*****************************************")
      return
    }

    val tiling=new SARS(args(0).toInt)

    tiling.writeCenters("test")

  }



}

