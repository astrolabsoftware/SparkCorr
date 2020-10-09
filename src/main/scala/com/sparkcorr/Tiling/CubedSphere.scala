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

import scala.collection.mutable.ArrayBuffer


class CubedSphere(nside:Int) extends SphereTiling with Serializable {

  val N:Int=nside

  val a=1/math.sqrt(3.0)
  val step=Pi/2/N

  /* position of nodes on a face*/
  /* for equal angle */
  val localPos:arr2[(Double,Double)] = {
    val facenodes=new arr2[(Double,Double)](N+1)
    val alpha=Array.tabulate(N+1)(i=>i*step-Pi/4)
      for (i<-0 to N; j<-0 to N){
        val x=a*math.tan(alpha(i))
        val y=a*math.tan(alpha(j))
        facenodes(i,j)=(x,y)
      }
    facenodes
  }


  /** for equal angles */
  def ang2LocalIndex(face:Int,theta:Double,phi:Double):(Int,Int)= {
    val (x,y)=ang2Local(face)(theta,phi)
    val alpha=math.atan(x)
    val beta=math.atan(y)
    val i:Int=math.floor((alpha+Pi/4)/step).toInt
    val j:Int=math.floor((beta+Pi/4)/step).toInt
    (i,j)
  }
  


  /** compute equal-angle nodes */
  def buildNodes():Array[arr2[Point3D]]={
    /** project coordinates from face to unit sphere. index is the face */
    val projector=new Array[(Double,Double)=>(Double,Double,Double)](6)
    projector(0)=(x,y)=>{val r=math.sqrt(a*a+x*x+y*y); (a/r,x/r,y/r)}
    projector(1)=(x,y)=>{val r=math.sqrt(a*a+x*x+y*y);(-x/r,a/r,y/r)}
    projector(2)=(x,y)=>{val r=math.sqrt(a*a+x*x+y*y);(-a/r,-x/r,y/r)}
    projector(3)=(x,y)=>{val r=math.sqrt(a*a+x*x+y*y);(x/r,-a/r,y/r)}
    projector(4)=(x,y)=>{val r=math.sqrt(a*a+x*x+y*y);(-y/r,x/r,a/r)}
    projector(5)=(x,y)=>{val r=math.sqrt(a*a+x*x+y*y);(y/r,x/r,-a/r)}

    val nodes=new Array[arr2[Point3D]](6)
    
    //build nodes/pixels
    for (face <- 0 to 5) {
      val proj=projector(face)
      val facenodes=new arr2[Point3D](N+1)
      //fill nodes for this face
      for (i<-0 to N; j<-0 to N){
        val (x,y)=localPos(i,j)
        val XYZ=proj(x,y)
        val p=new Point3D(XYZ._1,XYZ._2,XYZ._3)
        facenodes(i,j)=p
      }
      nodes(face)=facenodes
    }
    nodes
  }

  //pixel radius (in rad)
  val Rsq:Double=sqrt(2*Pi/6)/N
  var radii=new ArrayBuffer[Double] 
  def Rmin():Double=radii.min*Rsq
  def Rmax():Double=radii.max*Rsq

  //N below which all pix radius are greater than R
  //R in arcmin
  def pixRadiusGt(R:Double):Int = {
    val Nsq:Double=toDegrees(sqrt(Pi/3)/R)*60
    val N:Int=floor(Nsq*Rmin()).toInt
     N-N%2
  }

  //N above which all pixels have radii lower than R
  //R in arcmin
  def pixRadiusLt(R:Double):Int = {
    val Nsq=toDegrees(sqrt(Pi/3)/R)*60
    val N=ceil(Nsq*Rmax()).toInt
    N+N%2
  }

  /* compute pixel centers as barycenter of cells */
  def buildPixels(nodes:Array[arr2[Point3D]]):Array[(Double,Double)]={

    require(nodes.size==6)
    val pixarray=new Array[(Double,Double)](N*N*10-4)

    for (face <- 0 to 5) {
      val facenodes=nodes(face)
      //compute centers as cell barycenter
      for(i<-0 until N;j<-0 until N){
        val cell=facenodes(i,j)::facenodes(i+1,j)::facenodes(i,j+1)::facenodes(i+1,j+1)::Nil
        val bary=Point.barycenter(cell)
        val cen=new Point3D(bary/bary.norm())
        radii+=cell.map(c=>c.dist(cen)).max/Rsq
        val ipix:Int=coord2pix(face,i,j)
        pixarray(ipix)=cen.unitAngle
      }

    }// end face
    println(f"pixel radii: Rmin=${Rmin()/Rsq}%5.3f Rmax=${Rmax()/Rsq}%5.3f (Rsq=${toDegrees(Rsq)*60}%3.2f arcim)")
    pixarray
  }

  // array for fast access
  val pixcenter:Array[(Double,Double)]=buildPixels(buildNodes)

  /** pixel numbering
    * not continous (do not assume it is in the [0,6N^2-1] range, it is not)
    * you should access the valid indices with the following function
    */
  override val pixNums:IndexedSeq[Int]=
    for {f <- 0 to 5; i<-0 until N; j<-0 until N} yield coord2pix(f,i,j)

  /**transformationm to/from (face,i,j) */
  def coord2pix(f:Int,i:Int,j:Int):Int= (i*N+j)*10+f
  def pix2coord(ipix:Int):(Int,Int,Int)= { val ij=ipix/10; (ipix%10,ij/N,ij%N)}
  /** check */
  def isValidPix(ipix:Int):Boolean={val (f,i,j)=pix2coord(ipix); f<6 & i<N & j<N}

  /** get pixel centers 
  * output is a (theta,phi) tuple with 0<theta<pi, 0<phi<2pi
    */ 
  override def pix2ang(ipix:Int):Array[Double]= { 
    val (t:Double,f:Double)=pixcenter(ipix)
    Array(t,f)
  }

  /** find pixel number corresponding to a given direction 
    *  for the equal angle case
    *  use classical spherical coordinates, ie. 0<theta<Pi and 0<phi<2Pi
    */
  override def ang2pix(theta:Double,phi:Double):Int = {
    val face:Int=getFace(theta,phi)
    val (i,j)=ang2LocalIndex(face,theta,phi)
    coord2pix(face,i,j)
  }

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


  def getFace(theta:Double,phi:Double):Int={
    val testface=((phi+Pi/4)%(2*Pi)/(Pi/2)).toInt
    val y=ang2Local_y(testface)(theta,phi)

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



/** get pixel neighbours. yes that's pretty painfull for the borders but not
  *  a big deal since I code bug-free. (yes this was checked)
  */
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

  /** get the pixel neighbors (generally 8 sometimes 7) */
  def neighbours(ipix:Int):List[Int]= {
    val (f:Int,i:Int,j:Int)=pix2coord(ipix)

    val n:List[(Int,Int,Int)]=
      (f,i,j+1)::(f,i,j-1)::(f,i+1,j)::(f,i+1,j+1)::(f,i+1,j-1)::(f,i-1,j)::(f,i-1,j+1)::(f,i-1,j-1)::Nil

    val p=n.map(wrapcoord)

    p.map{case (f:Int,i:Int,j:Int)=>coord2pix(f,i,j)}.distinct

  }

  //fixed size array (8) adding -1 if only 7 neighbors
  override def neighbours8(ipix:Int):Array[Int]= {
    val a=neighbours(ipix).toArray
    if (a.size==8) a else a:+ -1
  }




  /** construct nodes on the sphere with a given strategy 
    *  here equal angles for each point on a 
    *  face viewed from the center 
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

  def writeRadii(fn:String):Unit={

    val writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fn,false)))
    for (r<- radii) {
      val s=f"$r%f\n"
      writer.write(s)
    }

  writer.close
  println(fn+ " written")

   }
 
 def writeNeighbours(ipix:Int):Unit={

   val fn="neighbours.txt"
   val writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fn,false)))

   val n=neighbours(ipix)
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

object CubedSphere {

  /** utility to benchmarks */
  def time[R](block: => R) = {
    def print_result(s: String, ns: Long) = {
      val formatter = java.text.NumberFormat.getIntegerInstance
      println("%-16s".format(s) + formatter.format(ns) + " ms")
    }

    var t0 = System.nanoTime()/1000000
    var result = block    // call-by-name
    var t1 = System.nanoTime()/1000000

    print_result("First Run", (t1 - t0))

    var lst = for (i <- 1 to 5) yield {
      t0 = System.nanoTime()/1000000
      result = block    // call-by-name
      t1 = System.nanoTime()/1000000
      print_result("Run #" + i, (t1 - t0))
      (t1 - t0).toLong
    }

    println("------------------------")
    print_result("Max", lst.max)
    print_result("Min", lst.min)
    print_result("Avg", (lst.sum / lst.length))
}

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


    c.writeCenters(s"EACScenters$N.txt")
    c.writeRadii(s"EACSradii$N.txt")

      /*
    val f=args(1).toInt
    val i=args(2).toInt
    val j=args(3).toInt
    tiling.writeNeighbours(tiling.coord2pix(f,i,j))
       */


  }
}
