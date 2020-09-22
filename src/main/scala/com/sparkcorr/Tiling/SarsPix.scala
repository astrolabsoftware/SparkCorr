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

import scala.math.{Pi,sqrt,cos,sin,acos}

import org.apache.log4j.{Level, Logger}

import java.io._
import java.util.Locale




class SarsPix(nside:Int) extends CubedSphere(nside) {


  require(nside%2==0,"nside for SARS must be even")

  //Nodes construction
  override  def buildNodes():Array[arr2[Point]]={

    val nodes=new Array[arr2[Point]](6)

    //face0

    //subfaces
    val M0:arr2[Point3D]=subface0(1,1)
    val M1:arr2[Point3D]=subface0(1,-1)
    val M2:arr2[Point3D]=subface0(-1,1)
    val M3:arr2[Point3D]=subface0(-1,-1)



    nodes
  }

    //build sufaces for face 0
  def subface0(signa:Int,signb:Int):arr2[Point3D] = {
    val M=new arr2[Point3D](N/2+1)

    M(0,0)=new Point3D(0.0,0.0)
    for (i <- 1 to N/2){
        val gi=Pi/12*((i/N)*(i/N))+Pi/4
        val alpha_i=signa*acos(sqrt(2.0)*cos(gi))
        M(i,0)=new Point3D(alpha_i,0)
        val beta_ii=signb*acos(1/(sqrt(2.0)*sin(gi)))
        M(i,i)=new Point3D(alpha_i,beta_ii)
        for (j <- 0 to i) {
            val beta_ij=j*beta_ii/i
            M(i,j)=new Point3D(alpha_i,beta_ij)
        }
    }
    //symetrize
    for (i <- 1 to N/2){
        for (j <- 0 until i){
            M(j,i)=new Point3D(M(i,j).x,signa*signb*M(i,j).z,signa*signb*M(i,j).y)
        }
    }
    M
  }


  //get subfaceindex from face index
  //I,J = face index
  def subfaceIndex(I:Int,J:Int):(Int,Int,Int)=
    if (J<=N)
        if (I<=N) (3,N-I,N-J) else (1,I-N,N-J)
    else
        if (I<=N) (2,N-I,J-N) else (0,I-N,J-N)

  //define rotations on X,Y,Z from face 0 to any up to 5
  val rotations=new Array[(Double,Double,Double)=>(Double,Double,Double)](6)
  rotations(0)=(x,y,z)=>(x,y,z)
  rotations(1)=(x,y,z)=>(-y,x,z)
  rotations(2)=(x,y,z)=>(-x,-y,z)
  rotations(3)=(x,y,z)=>(y,-x,z)
  rotations(4)=(x,y,z)=>(z,y,-x)
  rotations(5)=(x,y,z)=>(-z,y,x)


  //rotates face0 onto fnum
  def rotateFace0(face0:arr2[Point3D],fnum:Int):arr2[Point3D]={
    val face=new arr2[Point3D](face0.size)
    val rot=rotations(fnum)
    for ( i <- 0 until face0.size) 
    {
      for ( j <- 0 until face0.size)
      {
        val p:Point3D=face0(i,j)
        val (x,y,z)=rot(p.x,p.y,p.z)
        face(i,j)=new Point3D(x,y,z)
      }
    }
    face
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
object SarsPix {


  def main(args:Array[String]):Unit= {

    if (args.size!=1){
      println("*****************************************")
      println(">>>> Usage: SARS nside")
      println("*****************************************")
      return
    }

    val tiling=new SarsPix(args(0).toInt)

    tiling.writeCenters("test")

  }



}

