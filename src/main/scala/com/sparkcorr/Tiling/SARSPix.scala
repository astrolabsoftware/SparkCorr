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

import scala.math.{Pi,sqrt,cos,sin,acos,toDegrees,abs}

import org.apache.log4j.{Level, Logger}

import java.io._
import java.util.Locale


class SARSPix(nside:Int) extends CubedSphere(nside) {


  require(nside%2==0,"nside for SARS must be even")

  //Nodes construction
  override  def buildNodes():Array[arr2[Point3D]]={

    val nodes=new Array[arr2[Point3D]](6)

    //build FACE0
    //quadrants
    val q0:arr2[Point3D]=newF0Quadrant(0)
    val q1:arr2[Point3D]=newF0Quadrant(1)
    val q2:arr2[Point3D]=newF0Quadrant(2)
    val q3:arr2[Point3D]=newF0Quadrant(3)

    //concatenate using face index
    nodes(0)=new arr2[Point3D](N+1)
    val f0:Array[arr2[Point3D]]=Array(q0,q1,q2,q3)
      for (I <- 0 to N) {
        for (J <- 0 to N) {
          val (q,i,j)=face2localNodeIndex(I,J)
          nodes(0)(I,J)=f0(q)(i,j)
        }
      }

    //now build other faces by rotating FACE0
    //partially aplied
    val rot0=rotateFace0(nodes(0))(_)

    nodes(1)=rot0(1)
    nodes(2)=rot0(2)
    nodes(3)=rot0(3)
    nodes(4)=rot0(4)
    nodes(5)=rot0(5)

    nodes
  }

    //build sufaces for face 0
  def newF0Quadrant(q:Int):arr2[Point3D] = {

    val (signa,signb):(Int,Int)=q match {
      case 0 => (1,1)
      case 1 => (1,-1)
      case 2 => (-1,1)
      case 3 => (-1,-1)
    }

    val M=new arr2[Point3D](N/2+1)
    val n=N/2
    //println(s"\ncall to newF0Quadrant q=$q n=$n")
    M(0,0)=new Point3D(Pi/2,0.0)

    for (i <- 1 to N/2){
      val gi:Double=Pi/12*(i.toDouble/n)*(i.toDouble/n)+Pi/4
      val alpha_i:Double=signa*acos(sqrt(2.0)*cos(gi))
      M(i,0)=new Point3D(Pi/2,alpha_i)
      val beta_ii:Double=signb*acos(1/(sqrt(2.0)*sin(gi)))
      //if (q==0) println(s"node:i=$i, gi=$gi, ai=${toDegrees(alpha_i)} bii=${toDegrees(beta_ii)}")
      M(i,i)=new Point3D(Pi/2-beta_ii,alpha_i)
      for (j <- 0 to i) {
        val beta_ij:Double=j*beta_ii/i
        M(i,j)=new Point3D(Pi/2-beta_ij,alpha_i)
        //if (q==0) println(s"\tnode:(i=$i,j=$j) ai=${toDegrees(alpha_i)} bij=${toDegrees(beta_ij)}")
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


  // convert face (I,J) coordinates to local (q,i,j) ones
  def face2localNodeIndex(I:Int,J:Int):(Int,Int,Int)={
    if (J<=N/2)
        if (I<=N/2) (3,N/2-I,N/2-J) else (1,I-N/2,N/2-J)
    else
        if (I<=N/2) (2,N/2-I,J-N/2) else (0,I-N/2,J-N/2)
  }

  def face2localBinIndex(I:Int,J:Int):(Int,Int,Int)={
    if (J<=N/2-1)
        if (I<=N/2-1) (3,N/2-1-I,N/2-1-J) else (1,I-N/2,N/2-J-1)
    else
        if (I<=N/2-1) (2,N/2-I-1,J-N/2) else (0,I-N/2,J-N/2)
  }


  // convert local (q,i,j) coordinates to face ones (I,J)
  def local2faceNodeIndex(q:Int,i:Int,j:Int):(Int,Int)= q match {
    case 0 => (i+N/2,j+N/2)
    case 1 => (i+N/2,N/2-j)
    case 2 => (N/2-i,N/2+j)
    case 3 => (N/2-i,N/2-j)
  }

  //same for bin number
  def local2faceBinIndex(q:Int,i:Int,j:Int):(Int,Int)= q match {
    case 3 => (N/2-1-i,N/2-1-j)
    case 1 => (N/2+i,N/2-1-j)
    case 2 => (N/2-1-i,N/2+j)
    case 0 => (i+N/2,j+N/2)
  }

  //rotates face0 onto fnum
  def rotateFace0(face0:arr2[Point3D])(fnum:Int):arr2[Point3D]={

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

    implicit def bool2int(b:Boolean):Int = if (b) 1 else 0
    (z0<0)*(1<<0)+(y0<0)*(1<<1)
  }

  def getFaceQuadrant(p:Point3D):(Int,Int)={
    val face:Int=getFace(p)
    val q:Int=getQuadrant(face,p)
    (face,q)
  }

  //returns local coordinates (f,q,i,j)
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

    /*
    var (i,j)=index0(p0)
    if (j>i) {
      //println("swap coordinates")
      val symp=new Point3D(x0,signa*signb*z0,signa*signb*y0)
      val (symi,symj)=index0(symp)
      i=symj
      j=symi
    }
    //println(s"i=$i j=$j")
     */


    //output
    (face,q,i,j)
  }

  override def ang2pix(theta:Double,phi:Double):Int = {
    
    val p=new Point3D(theta,phi)
    val (f,q,i,j)=getLocalIndex(p)
    //face index
    val (ii,jj)=local2faceBinIndex(q,i,j)
    //println(s"f=$f q=$q i=$i j=$j I=$ii J=$jj")
    //pixel index
    coord2pix(f,ii,jj)
  }

}



// companion
object SARSPix {

  def main(args:Array[String]):Unit= {
    Locale.setDefault(Locale.US)


    if (args.size!=1){
      println("*****************************************")
      println(">>>> Usage: SARSPix N")
      println("*****************************************")
      return
    }


   // Set verbosity
    Logger.getLogger("org").setLevel(Level.WARN)
    Logger.getLogger("akka").setLevel(Level.WARN)
    Locale.setDefault(Locale.US)

    val N=args(0).toInt
    println(s"-> Constructing SARS sphere of size $N Npix=${6*N*N/1000000.0} M")

    val c=new SARSPix(args(0).toInt)

    //basic test
    for (ipix<-c.pixNums) {
      val (f,ii,jj)=c.pix2coord(ipix)
          //coords
          val (q,i,j)=c.face2localBinIndex(ii,jj)
          //println(s"\npix=$ipix f=$f q=$q ($i,$j)")
          //pos
          val Array(theta,phi)=c.pix2ang(ipix)
          //ang2pix
          val p=new Point3D(theta,phi)
          //println(s"center=  ai=${toDegrees(phi)} bij=${toDegrees(Pi/2-theta)} $p")
          val (fb,qb,ib,jb)=c.getLocalIndex(p)
          //println(s"back f=$fb q=$qb ($ib,$jb)")
          require(f==fb & qb==q & ib==i & jb==j)
    }

    c.writeCenters(s"SARScenters$N.txt")
    c.writeRadii(s"SARSradii$N.txt")


      /*
    val fc=args(1).toInt
    val ic=args(2).toInt
    val jc=args(3).toInt

    c.writeNeighbours(c.coord2pix(fc,ic,jc))

    val ipix=c.coord2pix(fc,ic,jc)

    val Array(tc,phic)=c.pix2ang(ipix)
    val pcen=new Point3D(tc,phic)

    //println(s"input pixel=$ipix ($fc,$ic,$jc)  angles=($tc,$phic) :"+pcen)


    val n=c.neighbours(ipix)
    for (in <- n) {
      val (f,i,j)=c.pix2coord(in)
      val ang=c.pix2ang(in)
      val p=new Point3D(ang(0),ang(1))
      //println(s"voisin pixel=$in ($f,$i,$j): angles=${ang(0)},${ang(1)} "+p)
    }
       */
  } //main


}//object
