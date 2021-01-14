/*
 * Copyright 2020 AstroLab SoftwareS
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


/** Add serialization to external Pointing objects*/
class ExtPointing extends Pointing with java.io.Serializable

/**
  *  Encapsulation of some Healpix functions within [[SphereTiling]] abstract class
  * 
  * you will need to build (or get) the java Healpix library see [[https://github.com/cds-astro/cds-healpix-java]]
  * 
  * @author S.Plaszczynski and J.Peloton
  */
class HealpixGrid(hp : HealpixBase, ptg : ExtPointing) extends SphereTiling with Serializable{

   override def pixNums:IndexedSeq[Int]=
    for {i <- 0 until hp.getNpix().toInt} yield i

  override def ang2pix(theta : Double, phi : Double) : Int = {
    ptg.theta = theta
    ptg.phi = phi
    hp.ang2pix(ptg).toInt
  }
  override def neighbours(ipix:Int):Array[Int] =  {
    hp.neighbours(ipix.toLong).map(_.toInt).filter(_ != -1)
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
  val Npix=12L*Nbase*Nbase
}

/**
  * Implementation of pixel properties for the Healpix tiling 
  * 
  *  @constructor creates a dedicated PixProps for Healpix
  *  @param Rin inner radius in arcmin
  *  @param Rout outer radius in arcmin
  *  
  *  @see [[https://arxiv.org/abs/2012.08455]] for inner/outer radii
  */
class HealpixProps(val Rin:Double,val Rout:Double)  extends PixProps {
  /**
    * nside below which all pixel radii are greater than R
  
    *  @param R radius in arcmin
    */
  def pixRadiusGt(R:Double):Int = {
    val Nsq:Double=toDegrees(sqrt(Pi/12)/R)*60
    val N:Int=floor(log(Nsq*Rin)/log(2.0)).toInt
    val nside=1<<N
    nside
  }

  //N above which all pixels have radii lower than R
  //R in arcmin
  /**
    * nside above which all pixel radii are lower than R
  
    *  @param R radius in arcmin
    */
  def pixRadiusLt(R:Double):Int = {
    val Nsq=toDegrees(sqrt(Pi/6)/R)*60
    val N=ceil(log(Nsq*Rout)/log(2.0)).toInt
    val nside=1<<N
    nside
  }

  /**
    * number of pixels
    */
  def Npix(nside:Int):Long=12L*nside*nside

}

/**
  * companion object for simple factory creation 
  */
object HealpixGrid extends HealpixProps(0.65,1.48) {
  /** most simple way for creating a Healpix tiling
    * 
    * @param nside healpix resolution
    * @param sch healpix scheme, ie RING or NESTED
    * 
    *  @example val grid=HealpixGrid(1024, healpix.essentials.Scheme.NESTED)
    */
  def apply(nside:Long,sch:Scheme)=new HealpixGrid(new HealpixBase(nside, sch), new ExtPointing)


}
