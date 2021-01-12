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



/** Utility class for a 2D squared array of same type objects
  *  @constructor create a NxN matrix of type A objects
  *  @param N size in one dimension (the other is automatically the same)
  *  @example {{{
  *  // some object:
  *  case class Point(x:Double,y:Double,z:Double)
  *  p1=Point(1,2,3)
  *  // here is the arr2:
  *  val a=arr2[Point](5)
  *  // enter the matrix (note: do not need double parenthesis)
  *  a(0,0)=p1
  *  // access the element
  *  print(a(0,0))
  *  }}}
  *  @author Stephane Plaszczynski
*/
class arr2[A: Manifest](N:Int) extends Serializable {
  val size=N
  /** the actual stored array */
  val a=Array.ofDim[A](N,N)

  /** read elements with single parenthesis, as `print(a(0,0))` */
  def apply(i:Int,j:Int):A=a(i)(j)

  /** write  elements with single parenthesis, as `a(0,0)=2` */
  def update(i:Int,j:Int,v:A)=a(i)(j)=v

}
object arr2 {
  def apply[A:Manifest](n:Int)=new arr2[A](n)
}
