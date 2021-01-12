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

import scala.math.{Pi,sqrt,ceil,floor,toDegrees}

/** Properties for the Cubed Sphere (and derived) pixelization 
  *  @constructor minimal inner and maximal outer radii
  *  @param Rin minimal inner radius in arcmin
  *  @param Rout maximal outer radius in arcmin
  *  @see [[https://arxiv.org/abs/2012.08455]] for values for [[CubedSphere]] and [[SARSPix]]
  */
class CubedProps(val Rin:Double, val Rout:Double) extends PixProps{

  //N below which all pix radius are greater than R
  //R in arcmin
  def pixRadiusGt(R:Double):Int = {
    val Nsq:Double=toDegrees(sqrt(Pi/6)/R)*60
    val N:Int=floor(Nsq*Rin).toInt
     N-N%2
  }

  //N above which all pixels have radii lower than R
  //R in arcmin
  def pixRadiusLt(R:Double):Int = {
    val Nsq=toDegrees(sqrt(Pi/3)/R)*60
    val N=ceil(Nsq*Rout).toInt
    N+N%2
  }

  def Npix(nbase:Int):Long= 6L*nbase*nbase
}

