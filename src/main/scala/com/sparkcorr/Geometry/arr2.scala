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


//@ val m=Map[Int,(Double,Double)=>(Double,Double,Double)]() 

//* a simple utility for 2D squared arrays */

class arr2(N:Int) {
  val size=N
  val a=Array.ofDim[Double](N,N)

  //elements access through parentheses
  def apply(i:Int):Array[Double]=a(i)
  def apply(i:Int,j:Int):Double=a(i)(j)

}
object arr2 {
  def apply(n:Int)=new arr2(n)
}
