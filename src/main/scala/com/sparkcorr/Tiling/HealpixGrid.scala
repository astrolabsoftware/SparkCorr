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


import healpix.essentials.HealpixBase
import healpix.essentials.Pointing
import healpix.essentials.Vec3
import healpix.essentials.Scheme
import scala.math.{log,toDegrees,floor,ceil,Pi,sqrt}

class ExtPointing extends Pointing with java.io.Serializable

class HealpixGrid(hp : HealpixBase, ptg : ExtPointing) extends SphereTiling with Serializable{

   override val pixNums:IndexedSeq[Int]=
    for {i <- 0 until hp.getNpix().toInt} yield i

  override def ang2pix(theta : Double, phi : Double) : Int = {
    ptg.theta = theta
    ptg.phi = phi
    hp.ang2pix(ptg).toInt
  }
  override def neighbours8(ipix:Int):Array[Int] =  {
    hp.neighbours(ipix.toLong).map(_.toInt)
  }
  override def pix2ang(ipix:Int):Array[Double]=
  {
    val p:Pointing=hp.pix2ang(ipix.toLong)
    Array(p.theta,p.phi)
  }

  val Nbase=hp.getNside
  val Npix=12*Nbase*Nbase

}

//companion for simple factory
object HealpixGrid {

  def apply(nside:Long,sch:Scheme)=new HealpixGrid(new HealpixBase(nside, sch), new ExtPointing)


  val minmaxRadius=(0.97,1.47)

//N below which all pix radius are greater than R
  //R in arcmin
  def pixRadiusGt(R:Double):Int = {
    val Rmin=minmaxRadius._1
    val Nsq:Double=toDegrees(sqrt(Pi/6)/R)*60
    val N:Int=floor(log(Nsq*Rmin)/log(2.0)).toInt
    val nside=1<<N
    nside
  }
  //N above which all pixels have radii lower than R
  //R in arcmin
  def pixRadiusLt(R:Double):Int = {
    val Rmax=minmaxRadius._2
    val Nsq=toDegrees(sqrt(Pi/6)/R)*60
    val N=ceil(log(Nsq*Rmax)/log(2.0)).toInt
    val nside=1<<N
    nside
  }

  def Npix(nside:Int):Int=12*nside*nside

}
