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
import scala.math.{sqrt,tan,Pi,abs,cos,sin}

import org.apache.log4j.{Level, Logger}
import java.io._
import java.util.Locale

class CubedSphere(Nface:Int) {

  val N:Int=Nface

  //grid
  val nodes=new Array[arr2[Point]](6)
  val centers=new Array[arr2[Point3D]](6)

  // array for fast access
  val pixcenter=new Array[(Double,Double)](N*N*10-4)

  buildEqualAngleNodes()
  buildCenters()

 
  /** pixels numbering */

  /** use this function to get the list of valid pixels */
  def pixNums()=for {f <- 0 to 5; i<-0 until N; j<-0 until N} yield coord2pix(f,i,j)

  /**transformationm to/from (face,i,j) */
  def coord2pix(f:Int,i:Int,j:Int):Int= (i*N+j)*10+f
  def pix2coord(ipix:Int):(Int,Int,Int)= { val ij=ipix/10; (ipix%10,ij/N,ij%N)}
  /** check */
  def isValidPix(ipix:Int):Boolean={val (f,i,j)=pix2coord(ipix); f<6 & i<N & j<N}

  /** get pixel centers 
  * output is a (theta,phi) tuple
    */ 
  def pix2ang(ipix:Int):(Double,Double)= pixcenter(ipix)


  /** find pixel number corresponding to a given direction */
  def ang2pix(theta:Double,phi:Double):Int = {
    val face=getFace(theta,phi)
    val (x,y)=ang2cube(face)(Pi/2-theta,phi)
    0
  }

  /** (lat,lambda)=>(x,y)/a */
  val ang2cube=new Array[(Double,Double)=>(Double,Double)](6)
  ang2cube(0)=(t,l)=>(tan(l),tan(t)/cos(l))
  ang2cube(1)=(t,l)=>(-1/tan(l),tan(t)/sin(l))
  ang2cube(2)=(t,l)=>(tan(l),-tan(t)/cos(l))
  ang2cube(3)=(t,l)=>(-1/tan(l),-tan(t)/sin(l))
  ang2cube(4)=(t,l)=>(sin(l)/tan(t),-cos(l)/tan(t))
  ang2cube(5)=(t,l)=>(-sin(l)/tan(t),-cos(l)/tan(t))

  /** (X,Y,Z)=>(x,y)/a */

  def getFace(theta:Double,phi:Double):Int={
    //possible face around azimuth
    val testface=((phi+Pi/4)%(2*Pi)/(Pi/2)).toInt
    val t=tan(Pi/2-theta)
    val c=cos(phi)
    val s=sin(phi)
    val y= testface match {
      case 0 => t/c
      case 1 => t/s
      case 2 => -t/c
      case 3 => -t/s
    }
    //
    if (abs(y)<1) testface
    else if (y>1) 4  
    else 5
  }


  val a=1/sqrt(3.0)
  /** construct nodes on the sphere with a given strategy 
    *  here equal angles for each point on a 
    *  face viewed from the center 
    */
  def buildEqualAngleNodes()= {

    /** equal angles*/
    val alpha=Array.tabulate(N+1)(i=>i*Pi/(2*N)-Pi/4)

    /** project coordinates from face to unit sphere. index is the face */
    val projector=new Array[(Double,Double)=>(Double,Double,Double)](6)
    projector(0)=(x,y)=>{val r=sqrt(a*a+x*x+y*y); (a/r,x/r,y/r)}
    projector(1)=(x,y)=>{val r=sqrt(a*a+x*x+y*y);(-x/r,a/r,y/r)}
    projector(2)=(x,y)=>{val r=sqrt(a*a+x*x+y*y);(-a/r,-x/r,y/r)}
    projector(3)=(x,y)=>{val r=sqrt(a*a+x*x+y*y);(-x/r,-a/r,y/r)}
    projector(4)=(x,y)=>{val r=sqrt(a*a+x*x+y*y);(y/r,x/r,a/r)}
    projector(5)=(x,y)=>{val r=sqrt(a*a+x*x+y*y);(y/r,x/r,-a/r)}

    //build nodes
    for (f <- 0 to 5) {
      val proj=projector(f)
      val thisface=new arr2[Point](N+1)
      for (i<-0 to N; j<-0 to N){
        val x=a*tan(alpha(i))
        val y=a*tan(alpha(j))
        val XYZ=proj(x,y)
        val p=Point(XYZ._1,XYZ._2,XYZ._3)
        thisface(i,j)=p
      }
      nodes(f)=thisface
    }
  }


  /**construct centers as cells barycenter */
  def buildCenters()={
    for (f <- 0 to 5) {
      val face=new arr2[Point3D](N)
      for(i<-0 until N;j<-0 until N){
        val cell=nodes(f)(i,j)::nodes(f)(i+1,j)::nodes(f)(i,j+1)::nodes(f)(i+1,j+1)::Nil
        val bary=Point.barycenter(cell)
        face(i,j)=new Point3D(bary/bary.norm())
        val ipix:Int=coord2pix(f,i,j)
        pixcenter(ipix)=face(i,j).unitAngle
      }
      centers(f)=face
    }
  }

  //output centers
  def writeCenters(fn:String):Unit={

    val writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fn,false)))
    for (f <- 0 to 5; i<-0 until N; j<-0 until N){
      val p=centers(f)(i,j)
      val x=p.x
      val y=p.y
      val z=p.z
      val s=f"$f%d\t$x%f\t$y%f\t$z%f\n"
      writer.write(s)
    }

  writer.close
  println(fn+ " written")

   }
    //pixels numbering
  def writeAngles(fn:String):Unit={

    val writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fn,false)))
    for ( ipix <- pixNums) {
      val (tet,phi)=pix2ang(ipix)
      val s=f"$ipix%d\t$tet%f\t$phi%f\n"
      writer.write(s)
    }
  writer.close
  println(fn+ " written")

  }

}

object CubedSphere {

  def main(args:Array[String]):Unit= {
   // Set verbosity
    Logger.getLogger("org").setLevel(Level.WARN)
    Logger.getLogger("akka").setLevel(Level.WARN)
    Locale.setDefault(Locale.US)

    println("hello from CubedSphere")
    val tiling=new CubedSphere(args(0).toInt)
    tiling.writeCenters("centers.txt")
    tiling.writeAngles("tetphi.txt")



  }
}
