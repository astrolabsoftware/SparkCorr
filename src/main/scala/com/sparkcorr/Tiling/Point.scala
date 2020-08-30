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

import org.apache.log4j.{Level, Logger}

import scala.math.{atan2,acos}

//point nD
class Point (val coord:List[Double]){
  def this(l:Array[Double])=this(l.toList)

  val dim:Int=coord.length

  def apply(i:Int):Double=coord(i)

  def + (p:Point):Point= new Point((coord,p.coord).zipped.map(_+_))
  def - (p:Point):Point= new Point((coord,p.coord).zipped.map(_-_))
  def * (s:Double):Point= new Point(coord.map(x=>s*x))
  def / (s:Double):Point= new Point(coord.map(x=>x/s))
  def dot(p:Point):Double=(coord,p.coord).zipped.map(_ * _).sum

  def norm():Double=coord.map(x=>x*x).sum
  //def norm():Double=coord.reduceLeft((x1,x2)=>x1*x1+x2*x2)

  def dist2(p:Point)={
    require(p.dim==dim)
    //p.coord zip coord map(x=>(x._1-x._2)*(x._1-x._2)) sum 
    (coord,p.coord).zipped.map(_-_).map(x=>x*x).reduceLeft(_+_)
  }
  override def toString = "Point("+coord.mkString(",")+")"
} 

class Point2D(val x:Double,val y:Double) extends Point(x::y::Nil)

/** specialization for 3D case
  *  contains functions to deal with angles in space
  */
class Point3D(val x:Double,val y:Double,val z:Double) extends Point(x::y::z::Nil) {

  /** spherical coordinates : theta [0,pi] ,phi [0,2pi]
    * when you are sure point lies on unit sphere
    */
  def unitAngle():Tuple2[Double,Double]={
  (atan2(y,x),acos(z))
  }
  /** general case */
  def toAngle():Tuple3[Double,Double,Double]={
    val R=norm()
    (R,atan2(y,x),acos(z/R))
  }



}



//use companion factory method to create objects
object Point {
  def apply(c:Double*)=new Point(c.toList)

  //test
  def main(args:Array[String]):Unit= {

   // Set verbosity
    Logger.getLogger("org").setLevel(Level.WARN)
    Logger.getLogger("akka").setLevel(Level.WARN)

    println("point test")

    val p1=new Point3D(1,2,3)
    val p2=Point(3,4,5)
    println("p1="+p1)
    println(p1(0),p1(1),p1(2))
    println(p1.x,p1.y,p1.z)
    println(p1.toAngle)
    println("p2="+p2)
    println("sum="+(p1+p2))
    println("dist2="+p1.dist2(p2))
    println("p1 scaled by 10="+p1/10)



  }


}
