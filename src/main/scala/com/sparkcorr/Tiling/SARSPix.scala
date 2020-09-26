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


class SARSPix(nside:Int) extends CubedSphere(nside) {


  require(nside%2==0,"nside for SARS must be even")

  //Nodes construction
  override  def buildNodes():Array[arr2[Point3D]]={

    val nodes=new Array[arr2[Point3D]](6)

    //build FACE0
    //subfaces (quadrants)
    val q0:arr2[Point3D]=subface0(1,1)
    val q1:arr2[Point3D]=subface0(1,-1)
    val q2:arr2[Point3D]=subface0(-1,1)
    val q3:arr2[Point3D]=subface0(-1,-1)

    //concatenate subsfaces to local index
    val FACE0=new arr2[Point3D](N+1)
    val f0:Array[arr2[Point3D]]=Array(q0,q1,q2,q3)
      for (I <- 0 to N) {
        for (J <- 0 to N) {
          val (ff,i,j)=subfaceIndex(I,J)
          FACE0(I,J)=f0(ff)(i,j)
          //println(s"FACE0 ($I $J) = ${FACE0(ii,jj)}")
        }
      }

    nodes(0)=FACE0
    //now build other faces by rotating FACE0
    nodes(1)=rotateFace0(FACE0,1)
    nodes(2)=rotateFace0(FACE0,2)
    nodes(3)=rotateFace0(FACE0,3)
    nodes(4)=rotateFace0(FACE0,4)
    nodes(5)=rotateFace0(FACE0,5)

    nodes
  }

    //build sufaces for face 0
  def subface0(signa:Int,signb:Int):arr2[Point3D] = {
    val M=new arr2[Point3D](N/2+1)
    val n=N/2
    //println(s"\ncall to subface0 (signs=$signa,$signb) n=$n")
    M(0,0)=new Point3D(Pi/2,0.0)

    for (i <- 1 to N/2){
      val gi:Double=Pi/12*(i.toDouble/n)*(i.toDouble/n)+Pi/4
      val alpha_i:Double=signa*acos(sqrt(2.0)*cos(gi))
      //println(s"i=$i, gi=$gi, ai=$alpha_i")
      M(i,0)=new Point3D(Pi/2,alpha_i)
      val beta_ii:Double=signb*acos(1/(sqrt(2.0)*sin(gi)))
      M(i,i)=new Point3D(Pi/2-beta_ii,alpha_i)
      for (j <- 0 to i) {
        val beta_ij:Double=j*beta_ii/i
        M(i,j)=new Point3D(Pi/2-beta_ij,alpha_i)
        //println(s"subface0 (signs=$signa,$signb) = [$i,$j]=${M(i,j)}")
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
    if (J<=N/2)
        if (I<=N/2) (3,N/2-I,N/2-J) else (1,I-N/2,N/2-J)
    else
        if (I<=N/2) (2,N/2-I,J-N/2) else (0,I-N/2,J-N/2)

  //rotates face0 onto fnum
  def rotateFace0(face0:arr2[Point3D],fnum:Int):arr2[Point3D]={

    val face=new arr2[Point3D](face0.size)

    val rot=fnum match {
      case 0 => (x:Double,y:Double,z:Double)=>(x,y,z)
      case 1 => (x:Double,y:Double,z:Double)=>(-y,x,z)
      case 2 => (x:Double,y:Double,z:Double)=>(-x,-y,z)
      case 3 => (x:Double,y:Double,z:Double)=>(y,-x,z)
      case 4 => (x:Double,y:Double,z:Double)=>(-z,y,x)
      case 5 => (x:Double,y:Double,z:Double)=>(z,y,-x)

    }

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

  def getQuadrant(face:Int,p:Point3D):Int={
    val (x,y,z)=(p.x,p.y,p.z)
    // switch to face 0
    val (x0,y0,z0)= face match {
      case 0 => (x,y,z)
      case 1 => (y,-x,z)
      case 2 => (-x,-y,z)
      case 3 => (-y,x,z)
      case 4 => (z,y,-x)
      case 5 => (-z,y,x)
    }
    var q:Int=0
    if (z0<0) q+= 1<<0
    if (y0<0) q+= 1<<1

    q
  }

  def getFaceQuadrant(theta:Double,phi:Double):(Int,Int)={
    val face:Int=getFace(theta,phi)
    val q:Int=getQuadrant(face,new Point3D(theta,phi))
    (face,q)
  }


  /*
  override def ang2pix(theta:Double,phi:Double):Int = {
    val face:Int=getFace(theta,phi)

    val p=new Point3D(theta,phi)
    //subsface
    // subface index i,j

    //back to face index
    val (ii,jj)=(0,0)
    coord2pix(face,ii,jj)
  }
   */


}



// companion
object SARSPix {

  def main(args:Array[String]):Unit= {
    Locale.setDefault(Locale.US)


    if (args.size!=4){
      println("*****************************************")
      println(">>>> Usage: SARSPix nside f i j")
      println("*****************************************")
      return
    }


   // Set verbosity
    Logger.getLogger("org").setLevel(Level.WARN)
    Logger.getLogger("akka").setLevel(Level.WARN)
    Locale.setDefault(Locale.US)

    val N=args(0).toInt
    println(s"-> Constructing cubedsphere of size $N Npix=${6*N*N/1000000.0} M")

    val tiling=new SARSPix(args(0).toInt)


    tiling.writeCenters("centers.txt")

    val fc=args(1).toInt
    val ic=args(2).toInt
    val jc=args(3).toInt

    tiling.writeNeighbours(tiling.coord2pix(fc,ic,jc))

    val ipix=tiling.coord2pix(fc,ic,jc)

    val Array(tc,phic)=tiling.pix2ang(ipix)
    val pcen=new Point3D(tc,phic)

    println(s"input pixel=$ipix ($fc,$ic,$jc)  angles=($tc,$phic) :"+pcen)


    val n=tiling.neighbours(ipix)
    for (in <- n) {
      val (f,i,j)=tiling.pix2coord(in)
      val ang=tiling.pix2ang(in)
      val p=new Point3D(ang(0),ang(1))
      println(s"voisin pixel=$in ($f,$i,$j): angles=${ang(0)},${ang(1)} "+p)
    }

  }


}
