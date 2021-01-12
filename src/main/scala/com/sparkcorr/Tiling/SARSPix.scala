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

import com.sparkcorr.Geometry.{Point,Point3D}

import scala.math.{Pi,sqrt,cos,sin,acos,toDegrees,abs,floor,ceil}
import scala.collection.mutable.ArrayBuffer

import java.io._
import java.util.Locale

/** 
  * =The SimilAr Radius Sphere pixelization=
  *
  *  based on [[doc/partioningSphere.pdf]]
  *  This is not the full construction but the one described at the very
  *  beginning of section 3.3.
  * 
  *  It relies on the projection of the faces of an inscribed cube that
  *  are further subdivided into 4 quadrants. 
  *  Althought not having exact same areas pixels have quite compact
  *  inner and outer radii (see note).
  * 
  *  The face numbering convention is the following : 
  *  - 0 for x=1 plane, 
  *  - 1 for y=1 plane
  *  - 2 for x=-1
  *  - 3 for y=-1
  *  - 4 for z=1 (top) 
  *  - 5 for z=-1 (bottom).
  * 
  *  The quadrant numbering convention (on face 0) is
  * {{{
  *  | 2 | 0 |
  *  |---|---|
  *  | 3 | 1 |
  *  }}}
  * 
  *  - The ``local index`` is in the form (face,quadrant,i,j) where face is in [0,5] , 
  *  quadrant in [0,3] and (i,j) in [0,Nbase/2-1]
  *  - The ``face index`` is the [[CubedSphere]] one in the form (face,I,J)
  *  - ```(theta,phi)``` angles on the sphere are in radians in 
  *  classical spherical conventions, ie. 0<theta<Pi and 0<phi<2Pi
  * 
  *  There are 6 Nbase^2^ pixels per face and 6 Nbase^2^ 
  *  pixels on the entire sphere
  * 
  *  @example {{{
  *  val c=new SARSPix(10)
  *  for (ipix<-c.pixNums) {
  *      val Array(theta,phi)=c.pix2ang(ipix)
  *      val ipixb=c.ang2pix(theta,phi)
  *      require(ipix==ipixb)
  *  }
  *  }}}
  *  @constructor creates SARSpix tiling with resolution nbase
  *  @param nbase Number of points on one face in one dimension (even number)
  *  @note [[https://arxiv.org/abs/2012.08455]]
  *  @author Stephane Plaszczynski
  */
class SARSPix(val nbase:Int) extends CubedSphere(nbase) {


  require(nbase%2==0,"nside for SARS must be even")


  /** convert face (I,J) coordinates to local (q,i,j) ones for nodes numbers*/
  def face2localNodeIndex(I:Int,J:Int):(Int,Int,Int)={
    if (J<=N/2)
        if (I<=N/2) (3,N/2-I,N/2-J) else (1,I-N/2,N/2-J)
    else
        if (I<=N/2) (2,N/2-I,J-N/2) else (0,I-N/2,J-N/2)
  }

  /** convert face (I,J) coordinates to local (q,i,j) ones for bin numbers*/
  def face2localBinIndex(I:Int,J:Int):(Int,Int,Int)={
    if (J<=N/2-1)
        if (I<=N/2-1) (3,N/2-1-I,N/2-1-J) else (1,I-N/2,N/2-J-1)
    else
        if (I<=N/2-1) (2,N/2-I-1,J-N/2) else (0,I-N/2,J-N/2)
  }


  /** convert local (q,i,j) coordinates to face ones (I,J) for nodes*/
  def local2faceNodeIndex(q:Int,i:Int,j:Int):(Int,Int)= q match {
    case 0 => (i+N/2,j+N/2)
    case 1 => (i+N/2,N/2-j)
    case 2 => (N/2-i,N/2+j)
    case 3 => (N/2-i,N/2-j)
  }

  /** convert local (q,i,j) coordinates to face ones (I,J) for bin numbers*/
  def local2faceBinIndex(q:Int,i:Int,j:Int):(Int,Int)= q match {
    case 3 => (N/2-1-i,N/2-1-j)
    case 1 => (N/2+i,N/2-1-j)
    case 2 => (N/2-1-i,N/2+j)
    case 0 => (i+N/2,j+N/2)
  }

  /** get quadrant number from point p and face*/
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

    implicit def bool2int(b:Boolean):Int = if (b) 1 else 0
    (z0<0)*(1<<0)+(y0<0)*(1<<1)
  }

  /**
    *  get face and quadrant numbers for point p
    */
  def getFaceQuadrant(p:Point3D):(Int,Int)={
    val face:Int=getFace(p)
    val q:Int=getQuadrant(face,p)
    (face,q)
  }

  /**
    *  get local coordinates (f,q,i,j) for point p
    */
  def getLocalIndex(p:Point3D):(Int,Int,Int,Int)={

    val (x,y,z)=(p.x,p.y,p.z)

    //face
     val face:Int=getFace(p)

    //rotate to face 0
    val (x0,y0,z0)= face match {
      case 0 => (x,y,z)
      case 1 => (y,-x,z)
      case 2 => (-x,-y,z)
      case 3 => (-y,x,z)
      case 4 => (z,y,-x)
      case 5 => (-z,y,x)
    }
    //quadrant
    implicit def bool2int(b:Boolean):Int = if (b) 1 else 0
    val q=(z0<0)*(1<<0)+(y0<0)*(1<<1)

    //println(s"face=$face quadrant=$q")

    //local coordinates depends on q
    val (signa,signb):(Int,Int)=q match {
      case 0 => (1,1)
      case 1 => (1,-1)
      case 2 => (-1,1)
      case 3 => (-1,-1)
    }

    def index0(p:Point3D):(Int,Int)={
      val (tet,fi)=p.unitAngle()
      val ai=signa*fi
      val bij=(Pi/2-tet)
      val Gi=acos(cos(ai)/sqrt(2.0))
      val n=N/2
      val ii:Double=(n*sqrt(12/Pi*(Gi-Pi/4)))
      val i=ii.toInt
      val bii:Double=signb*acos(1/(sqrt(2.0)*sin(Gi)))

      //val j= if (abs(bii-bij)< 1e-12) i else ((i+1)*bij/bii).toInt
      val j= (ii*bij/bii).toInt
      //println(s"index0 for ai=${toDegrees(ai)}deg bij=${toDegrees(bij)}deg bii=${toDegrees(bii)} i=$i $j")
      (i,j)
    }

    val p0=new Point3D(x0,y0,z0)
    //println(s"point after rotation $p0")

    val (i,j)=index0(p0) match {
      case (i,j) if (j<=i) => (i,j)
      case _ => index0(new Point3D(x0,signa*signb*z0,signa*signb*y0)).swap
    }

    //output
    (face,q,i,j)
  }

  //local point on face 0
  private def getNode0(q:Int,itry:Int,jtry:Int):Point3D={
  
    if (itry==0 & jtry==0) {
      val p0=new Point3D(Pi/2,0.0)
      //println(s"node0 q=$q i=$itry j=$jtry $p0")
      return p0
    }
    //i must be lower than j
    val (i,j)=if (jtry<=itry) (itry,jtry) else (jtry,itry)

    val (signa,signb):(Int,Int)=q match {
      case 0 => (1,1)
      case 1 => (1,-1)
      case 2 => (-1,1)
      case 3 => (-1,-1)
    }

    val n=N/2
    val gi:Double=Pi/12*(i.toDouble/n)*(i.toDouble/n)+Pi/4
    val alpha_i:Double=signa*acos(sqrt(2.0)*cos(gi))
    if (j==0){
      val p=new Point3D(Pi/2,alpha_i)
      val pi0=(if (jtry<itry) p else new Point3D(p.x,signa*signb*p.z,signa*signb*p.y))
      //println(s"node0 q=$q i=$itry j=$jtry $pi0")
      return pi0
    }
    val beta_ii:Double=signb*acos(1/(sqrt(2.0)*sin(gi)))
    val beta_ij:Double=j*beta_ii/i
    val p=new Point3D(Pi/2-beta_ij,alpha_i)

    val pp=(if (jtry<=itry) p else new Point3D(p.x,signa*signb*p.z,signa*signb*p.y))
    //println(s"node0 q=$q i=$i j=$j $pp")
    pp
  }


  override def pix2ang(ipix:Int):Array[Double]={

    val (face,ii,jj)=pix2coord(ipix)
    
    val (q,i,j)=face2localBinIndex(ii,jj)

    //getnodes on face0
    val p1:Point3D=getNode0(q,i,j)
    val p2:Point3D=getNode0(q,(i+1),j)
    val p3:Point3D=getNode0(q,i,(j+1))
    val p4:Point3D=getNode0(q,i+1,(j+1))

    val cell:List[Point3D]=p1::p2::p3::p4::Nil
    val bary:Point=Point.barycenter(cell)
    val cen0=new Point3D(bary/bary.norm())

    //rotate to face
    val rot=face match {
      case 0 => (x:Double,y:Double,z:Double)=>(x,y,z)
      case 1 => (x:Double,y:Double,z:Double)=>(-y,x,z)
      case 2 => (x:Double,y:Double,z:Double)=>(-x,-y,z)
      case 3 => (x:Double,y:Double,z:Double)=>(y,-x,z)
      case 4 => (x:Double,y:Double,z:Double)=>(-z,y,x)
      case 5 => (x:Double,y:Double,z:Double)=>(z,y,-x)
    }

    val XYZ=rot(cen0.x,cen0.y,cen0.z)
    val cen=new Point3D(XYZ._1,XYZ._2,XYZ._3)
    val (t,f)=cen.unitAngle

    Array(t,f)


  }

  override def ang2pix(theta:Double,phi:Double):Int = {
    
    val p=new Point3D(theta,phi)
    val (f,q,i,j)=getLocalIndex(p)
    //protection for edges
    val ip=math.max(0,math.min(N/2-1,i))
    val jp=math.max(0,math.min(N/2-1,j))

    //face index
    val (ii,jj)=local2faceBinIndex(q,ip,jp)

    //pixel index
    val ipix=coord2pix(f,ii,jj)

    ipix
  }

}



// companion
object SARSPix extends CubedProps(0.82,1.1) {


  def main(args:Array[String]):Unit= {
    Locale.setDefault(Locale.US)


    if (args.size!=1){
      println("*****************************************")
      println(">>>> Usage: SARSPix N")
      println("*****************************************")
      return
    }


    Locale.setDefault(Locale.US)

    val N=args(0).toInt
    println(s"-> Constructing SARS sphere of size $N Npix=${6*N*N/1000000.0} M")

    val c=new SARSPix(args(0).toInt)

    //c.writeCenters(s"SARScenters$N.txt")
  } //main


}//object
