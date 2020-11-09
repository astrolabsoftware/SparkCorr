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
/**
  * Abstract class from which all tilings of the (unit) sphere 
  * should derive
  *  angles convention: in radians 0<theta<Pi, 0<phi<2*Pi
  */

abstract class SphereTiling {

  def pix2ang(ipix:Int):Array[Double]
  def ang2pix(theta:Double,phi:Double):Int

  //neighbours index array
  def neighbours(ipix:Int):Array[Int]


  //fixed size 8 array adding -1 if no : assumes 7 pixels min
  def neighbours8(ipix:Int):Array[Int]= {
    val a=neighbours(ipix)
    if (a.size==8) a else a:+ -1
  }

  //list of valid pixel indices
  val pixNums:IndexedSeq[Int]

  val Nbase:Int
  val Npix:Long

  //size of the array holding pixels
  val SIZE:Long

}
