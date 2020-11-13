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

import scala.math
import scala.math.{Pi,sqrt,toRadians,ceil,floor,toDegrees,abs}

import scala.util.Random

import org.apache.log4j.{Level, Logger}
import java.io._
import java.util.Locale

class CubedSphere(val Nbase:Int) extends SphereTiling with Serializable {

  val N:Int=Nbase
  val Npix:Long=6L*Nbase*Nbase
  val SIZE:Long=10L*Nbase*Nbase-4L

  val a=1/math.sqrt(3.0)
  val step=Pi/2/N


  /** pixel numbering
    * not continous (do not assume it is in the [0,6N^2-1] range, it is not)
    * you should access the valid indices with the following function
    */
  override def pixNums:IndexedSeq[Int]=
    for {f <- 0 to 5; i<-0 until N; j<-0 until N} yield coord2pix(f,i,j)

  /**transformationm to/from (face,i,j) */
  def coord2pix(f:Int,i:Int,j:Int):Int= (i*N+j)*10+f
  def pix2coord(ipix:Int):(Int,Int,Int)= { val ij=ipix/10; (ipix%10,ij/N,ij%N)}
  /** check */
  def isValidPix(ipix:Int):Boolean={val (f,i,j)=pix2coord(ipix); f<6 & i<N & j<N}

  /** get pixel centers 
  * output is a (theta,phi) tuple with 0<theta<pi, 0<phi<2pi
    */ 
 
  //pure function
  override def pix2ang(ipix:Int):Array[Double]= {
    val (face,i,j)=pix2coord(ipix)

     //equiangular
    val alpha=Array.tabulate(N+1)(i=>i*step-Pi/4)

    val (x1,y1)=(a*math.tan(alpha(i)),a*math.tan(alpha(j)))
    val (x2,y2)=(a*math.tan(alpha(i+1)),a*math.tan(alpha(j)))
    val (x3,y3)=(a*math.tan(alpha(i)),a*math.tan(alpha(j+1)))
    val (x4,y4)=(a*math.tan(alpha(i+1)),a*math.tan(alpha(j+1)))

    /** project coordinates from face to unit sphere. index is the face */
    val projector=new Array[(Double,Double)=>(Double,Double,Double)](6)
    projector(0)=(x,y)=>{val r=math.sqrt(a*a+x*x+y*y); (a/r,x/r,y/r)}
    projector(1)=(x,y)=>{val r=math.sqrt(a*a+x*x+y*y);(-x/r,a/r,y/r)}
    projector(2)=(x,y)=>{val r=math.sqrt(a*a+x*x+y*y);(-a/r,-x/r,y/r)}
    projector(3)=(x,y)=>{val r=math.sqrt(a*a+x*x+y*y);(x/r,-a/r,y/r)}
    projector(4)=(x,y)=>{val r=math.sqrt(a*a+x*x+y*y);(-y/r,x/r,a/r)}
    projector(5)=(x,y)=>{val r=math.sqrt(a*a+x*x+y*y);(y/r,x/r,-a/r)}

    val proj=projector(face)

    val XYZ1=proj(x1,y1)
    val p1=new Point3D(XYZ1._1,XYZ1._2,XYZ1._3)

    val XYZ2=proj(x2,y2)
    val p2=new Point3D(XYZ2._1,XYZ2._2,XYZ2._3)

    val XYZ3=proj(x3,y3)
    val p3=new Point3D(XYZ3._1,XYZ3._2,XYZ3._3)

    val XYZ4=proj(x4,y4)
    val p4=new Point3D(XYZ4._1,XYZ4._2,XYZ4._3)

    //println(f"ipix=$ipix ($face,$i,$j) from nodes ($x1%3.2f,$y1%3.2f) ($x2%3.2f,$y2%3.2f) ($x3%3.2f,$y3%3.2f) ($x4%3.2f,$y4%3.2f) ")


    val cell=p1::p2::p3::p4::Nil
    val bary=Point.barycenter(cell)
    val cen=new Point3D(bary/bary.norm())

    val (t,f)=cen.unitAngle

    Array(t,f)
  }



  /** find pixel number corresponding to a given direction 
    *  for the equal angle case
    *  use classical spherical coordinates, ie. 0<theta<Pi and 0<phi<2Pi
    */
  override def ang2pix(theta:Double,phi:Double):Int = {
    val face:Int=getFace(theta,phi)
    val (i,j)=ang2LocalIndex(face,theta,phi)
    //require(face>=0 & face<=5 & i>=0 & i<=N & j>=0 & j<=N,s"\n fail face=$face i=$i j=$j")
    coord2pix(face,i,j)
  }


  def getFace(theta:Double,phi:Double):Int={
    val testface=((phi+Pi/4)%(2*Pi)/(Pi/2)).toInt
    val y=CubedSphere.ang2Local_y(testface)(theta,phi)

    if (math.abs(y)<=1.0) testface
    else if (y>1) 4  
    else 5

  }

  // this is FP!
  def getFace(p:Point3D):Int={
    p.coord.zip(List((0,2),(1,3),(4,5))).foldLeft((0.0,0))( (acc,x)=> {val v =abs(x._1)
      if (v>acc._1)
        if (x._1>0) (v,x._2._1) else (v,x._2._2)
        else
          acc })._2

  }


  /** for equal angles */
  def ang2LocalIndex(face:Int,theta:Double,phi:Double):(Int,Int)= {
    val (x,y)=CubedSphere.ang2Local(face)(theta,phi)
    //protection
    val xx=math.max(-1.0,math.min(1.0,x))
    val yy=math.max(-1.0,math.min(1.0,y))
    val alpha=math.atan(xx)
    val beta=math.atan(yy)
    val i:Int=math.floor((alpha+Pi/4)/step).toInt
    val j:Int=math.floor((beta+Pi/4)/step).toInt
    (i,j)
  }
  


  /** get the pixel neighbors (generally 8 sometimes 7) */
  override def neighbours(ipix:Int):Array[Int]={
    val (f:Int,i:Int,j:Int)=pix2coord(ipix)

    val n:Array[(Int,Int,Int)]=Array(
      (f,i,j+1),(f,i,j-1),(f,i+1,j),(f,i+1,j+1),(f,i+1,j-1),(f,i-1,j),(f,i-1,j+1),(f,i-1,j-1)
    )

    //check pixel num is not falling on another face (ie treat edges+corners)
    def wrapcoord(c:(Int,Int,Int)):(Int,Int,Int)= {

      val xlim=(c._2>=0 & c._2<N)
      val ylim=(c._3>=0 & c._3<N)

      if (xlim & ylim)
        c

      val c1= if (!xlim) {
        c match {

          //top corners
          case(4,-1,-1)=> (0,0,N-1)
          case (4,N,N)=> (2,0,N-1)
          case (4,-1,N) => (3,0,N-1)
          case (4,N,-1) => (1,0,N-1)
        
          //bottom corners
          case (5,-1,-1) => (2,N-1,0)
          case (5,N,N) => (0,N-1,0)
          case (5,-1,N) => (3,N-1,0)
          case (5,N,-1) => (1,N-1,0)

          //top edges
          case (4,N,j)=> (1,j,N-1)
          case (4,-1,j) => (3,N-1-j,N-1)

          //bottom edges
          case (5,-1,j) => (3,j,0)
          case (5,N,j) => (1,N-1-j,0)

          //azimuthal faces
          case (f,N,j)=>((f+1)%4,0,j)
          case (f,-1,j)=>((f+3)%4,N-1,j)
        }
      } else c


      if (!ylim) {
        c1 match {
          case (0,i,N) => (4,i,0)
          case (0,i,-1) => (5,i,N-1)

          case (1,i,N) => (4,N-1,i)
          case (1,i,-1) => (5,N-1,N-1-i)

          case (2,i,N) => (4,N-1-i,N-1)
          case (2,i,-1) => (5,N-1-i,0)

          case (3,i,N) => (4,0,N-1-i)
          case (3,i,-1) => (5,0,i)

          case (4,i,-1) => (0,i,N-1)
          case (4,i,N) => (2,N-1-i,N-1)

          case (5,i,-1)=>(2,N-1-i,0)
          case (5,i,N) => (0,i,0)

          case _ => c1
        }
      }
      else
        c1


    }


    val p=n.map(wrapcoord)
    p.map{case (f:Int,i:Int,j:Int)=>coord2pix(f,i,j)}.distinct

  }


  /*
  val allneighbours=getallneighbours()

  def getallneighbours()={
    val n=new Array[Array[Int] ](SIZE)
    for (ipix <- pixNums) {
      n(ipix)=neighboursLoc(ipix)
    }
    n
  }

  override def neighbours(ipix:Int):Array[Int]= allneighbours(ipix)
   */

  //output centers
  def writeCenters(fn:String):Unit={

    val writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fn,false)))
    for (ipix <- pixNums) {
      val (f,i,j)=pix2coord(ipix)
      val ang=pix2ang(ipix)
      val p=new Point3D(ang(0),ang(1))
      val x=p.x
      val y=p.y
      val z=p.z
      val s=f"$f%d\t$x%f\t$y%f\t$z%f\n"
      writer.write(s)
    }

  writer.close
  println(fn+ " written")

   }

 
 def writeNeighbours(ipix:Int):Unit={

   val fn="neighbours.txt"
   val writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fn,false)))

   val n=neighbours(ipix).toList
    for (in <- List(ipix):::n) {
      val (f,i,j)=pix2coord(in)
      val ang=pix2ang(in)
      val p=new Point3D(ang(0),ang(1))
      val x=p.x
      val y=p.y
      val z=p.z
      val s=f"$f%d\t$i%d\t$j%d\t$x%f\t$y%f\t$z%f\n"
      writer.write(s)
    }

  writer.close
  println(fn+ " written")

 }
 

}

object CubedSphere extends CubedProps(0.77,1.26) {


  //static functions
  val ang2Local=new Array[(Double,Double)=>(Double,Double)](6)
  ang2Local(0)=(t,f)=>(math.tan(f),1.0/math.tan(t)/math.cos(f))
  ang2Local(1)=(t,f)=>(-1/math.tan(f),1.0/math.tan(t)/math.sin(f))
  ang2Local(2)=(t,f)=>(math.tan(f),-1.0/math.tan(t)/math.cos(f))
  ang2Local(3)=(t,f)=>(-1/math.tan(f),-1.0/math.tan(t)/math.sin(f))
  ang2Local(4)=(t,f)=>(math.sin(f)*math.tan(t),-math.cos(f)*math.tan(t))
  ang2Local(5)=(t,f)=>(-math.sin(f)*math.tan(t),-math.cos(f)*math.tan(t))


  //extract independent x/y functions
  val ang2Local_x=ang2Local.map{
     case (f:Function2[Double,Double,(Double,Double)]) => (x:Double,y:Double)=> f(x,y)._1}
  val ang2Local_y=ang2Local.map{
     case (f:Function2[Double,Double,(Double,Double)]) => (x:Double,y:Double)=> f(x,y)._2}

   

  def main(args:Array[String]):Unit= {


    if (args.size!=1){
      println("*****************************************")
      println(">>>> Usage: CubedSphere N")
      println("*****************************************")
      return
    }


   // Set verbosity
    Logger.getLogger("org").setLevel(Level.WARN)
    Logger.getLogger("akka").setLevel(Level.WARN)
    Locale.setDefault(Locale.US)

    val N=args(0).toInt
    println(s"-> Constructing cubedsphere of size $N Npix=${6*N*N/1000000.0} M")


    val c=new CubedSphere(args(0).toInt)


    //c.writeCenters(s"EACScenters$N.txt")

    for (ipix <- c.pixNums) {
      val (face,i,j)=c.pix2coord(ipix)
      val p1=c.pix2ang(ipix)
    }


      /*
    val f=args(1).toInt
    val i=args(2).toInt
    val j=args(3).toInt
    tiling.writeNeighbours(tiling.coord2pix(f,i,j))
       */


  }
}
