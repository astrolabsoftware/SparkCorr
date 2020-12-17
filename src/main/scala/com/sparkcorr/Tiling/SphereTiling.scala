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
  * (Abstract) base class for all tilings of the unit sphere
  *  There are 4 abstract functions to be implemented
  * 
  *  - pix2ang : from a pixel number determine the angles
  * 
  *  - ang2pix: from two angles dtermine the pixel number
  * 
  *  - neighbours : from a pixel number dtermine its neighbors indices
  * 
  *  - pixNums return the valid indices.
  * 
  *  All angles are in radians and 
  *  follow the mathematical convention for spherical coordinates
  *  ie. co-latiutde: 0<theta<Pi azimuth: 0<phi<2Pi
  * 
  * @author Stephane Plaszczynski
  * 
  */
abstract class SphereTiling {

  /**
    * pixel index center
    * 
    * @param ipix pixel index
    * @return size 2 Array of angles [theta,phi] (see header for conventions)  
    */
  def pix2ang(ipix:Int):Array[Double]


  /**
    * pixel index for these angles
    * 
    * @param theta colatitude (0<theta<Pi)
    * @param phi azimuth (0<phi<Pi)
    * @return pixel index
    */
  def ang2pix(theta:Double,phi:Double):Int

  /** neighbours index array
    * 
    * @param ipix pixel index
    * @return Array of pixel indices (variable size)
    */
  def neighbours(ipix:Int):Array[Int]


  /**
    *  automatic translation to 8 neighbouring indices 
    *  putting -1 if there is less.
    * 
    *  undefined behaviour if more than 8
    */
  def neighbours8(ipix:Int):Array[Int]= {
    val a=neighbours(ipix)
    if (a.size==8) a else a:+ -1
  }

  /** list of valid pixel indices
    * 
    * @return an indexedseq of indices
    */
  def pixNums:IndexedSeq[Int]

  /** resolution parameter */
  val Nbase:Int
  /** number of pixels */
  val Npix:Long

  /** size of the array holding pixels*/
  val SIZE:Long

}
