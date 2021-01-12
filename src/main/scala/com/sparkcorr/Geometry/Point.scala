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
package com.sparkcorr.Geometry

import scala.math.{Pi}

import scala.math


/**
  *  A generic class to deal with n-dimensional points (or vectors) in cartesian coordinates
  *  with some basic operations
  *  @constructor create point from list of coordinates. Companion object allows variable length arguments.
  *  @param coord coordinates list
  *  @example  {{{ val p=Point(1,2,3) 
  *  print(p)
  *  val x=p(0)
  *  val p2=p/2
  *  val sum=p+p2
  *  print(sum/10)
  *  }}}
  *  @author Stephane Plaszczynski
  */
class Point (val coord:List[Double]){

  /** auxiliary constructor from Array */
  def this(l:Array[Double])=this(l.toList)

  /** dimension */
  val dim:Int=coord.length

  /** direct access to coordinates 
    *  @example val p=new Point(1,2,3); p(0)=3
    * 
    */
  def apply(i:Int):Double=coord(i)

  /** addition */
  def + (p:Point):Point= new Point((coord,p.coord).zipped.map(_+_))

  /** substraction */
  def - (p:Point):Point= new Point((coord,p.coord).zipped.map(_-_))

  /** scale all coordinates  by ``s`` */ 
  def * (s:Double):Point= new Point(coord.map(x=>s*x))

  /** inverse-scale (divide) all coordinates by ``s`` */ 
  def / (s:Double):Point= new Point(coord.map(x=>x/s))

  /** dot product */
  def dot(p:Point):Double=(coord,p.coord).zipped.map(_ * _).sum

  /** norm of the vector */
  def norm():Double=math.sqrt(coord.map(x=>x*x).sum)
  //def norm():Double=coord.reduceLeft((x1,x2)=>x1*x1+x2*x2)

  /** squared distance to some other point */
  def dist2(p:Point):Double={
    require(p.dim==dim)
    //p.coord zip coord map(x=>(x._1-x._2)*(x._1-x._2)) sum 
    (coord,p.coord).zipped.map(_-_).map(x=>x*x).reduceLeft(_+_)
  }

  /** distance to other point */
  def dist(p:Point):Double=math.sqrt(dist2(p))

  /** pretty printing */
  override def toString = "Point("+coord.mkString(",")+")"
} 

/** Specialization of Point to 3D case.
  * 
  *  contains further functions to deal with corrresponding angles in space.
  *  @constructor create 3D point specifying the 3 cartesian coordinates.
  *  You can get access to them with `x`,`y` or `z` values
  *  @param x 1st coordinate
  *  @param y 2d coordinate
  *  @param z 3d coordinate
  *  @example {{{ 
  *  val p=Point(1,2,3)
  *  val pnorm=new Point3D(p/p.norm)
  *  val (r,theta,phi)=pnorm.toAngle()
  *  print(r,pnorm.x) 
  *  }}}
  */
class Point3D(val x:Double,val y:Double,val z:Double) extends Point(x::y::z::Nil) {

  /** conversion from a Point to Point3D */
  def this(p:Point)=this(p(0),p(1),p(2))

  /** create a 3D point from 2 angles on the unit sphere 
    *  @param theta co-latitude 0<theta<Pi
    *  @param phi longitude 0<phi<2Pi
    */
  def this(theta:Double,phi:Double)=this(math.cos(phi)*math.sin(theta),math.sin(phi)*math.sin(theta),math.cos(theta))

  /** Angles of the 3D point on the unit sphere 
    *  @return [theta,phi] tuple with 0<theta<Pi,0<phi<2Pi
    */
  def unitAngle():Tuple2[Double,Double]={
    val tet=math.acos(z)
    val phi=math.atan2(y,x)
    if (phi>0) (tet,phi) else (tet,phi+2*Pi)

  }
  /** spherical coordinates of the point 
    *  @return [R,theta,phi] tuple with 0<theta<Pi,0<phi<2Pi
    */
  def toAngle():Tuple3[Double,Double,Double]={
    val R=norm()
    val tet=math.acos(z)
    val phi=math.atan2(y,x)
    if (phi>0) (R,tet,phi) else (R,tet,phi+2*Pi)
  }



}


/** Point companion object for creation, test and static methods
  */
object Point {
  /** allows direct creation with variable length arguments
    * @example {{{val p=Point(3,4,5,6,7.2)}}}
    */
  def apply(c:Double*)=new Point(c.toList)

  /** compute the barycenter of a list of Points */
  def barycenter(l:List[Point]):Point = l.reduceLeft(_+_)/l.size

  /** some basic tests*/
  def main(args:Array[String]):Unit= {

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
