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
import com.sparkcorr.Geometry.Point3D

import org.scalatest.{BeforeAndAfter, FunSuite}
import scala.math.{Pi,abs,acos,sqrt,toRadians,toDegrees}
import scala.util.Random
import java.util.Locale

/**
  * Test class for SARSPix
  */
class SARSPixTest extends FunSuite with BeforeAndAfter {

  var c: SARSPix = _
  val N:Int= 4

  before {
    Locale.setDefault(Locale.US)
    c= new SARSPix(N)
  }

  test(s"construct object of size $N Npix=${6*N*N/1000000.0} M") {
    assert(true)
  }

   test("Numbers of pixels shoudl be 6N^2"){
    assert(c.pixNums.size==6*N*N)
  }


  test("Valid pixel numbers"){
    for (ipix<-c.pixNums) {
      assert(c.isValidPix(ipix)==true)
    }
  }
 

  test("Pixnum to coord") {
    for (ipix<-c.pixNums) {
      val (f,i,j)=c.pix2coord(ipix)
      assert(c.coord2pix(f,i,j)==ipix)
    }
  }


 test("Coord to pixnum") {
    for (f<-0 to 5; i<-0 until N; j<- 0 until N) {
      val ipix=c.coord2pix(f,i,j)
      assert(c.pix2coord(ipix)==(f,i,j))
    }
  }


  test("pix2ang returns angles in the correct range") {
    for (ipix<-c.pixNums) {
      val Array(theta,phi)=c.pix2ang(ipix)
      assert(theta>=0 & theta<=Pi & phi>=0 & phi<=2*Pi,f"theta=$theta phi=$phi")
    }
  }


  test("face2localIndex + local2faceIndex") {
    for (I <- 0 until c.N) {
      for (J <- 0 until c.N) {
        val (q,i,j)=c.face2localIndex(I,J)
        assert(c.local2faceIndex(q,i,j)==(I,J))
      }
    }

  }


  test("Faces and quadrants") {
 
    //azimuthal
    for (f <- 0 to 3) {
      val f0=f*Pi/2
      val t0=Pi/2
      assert(c.getFaceQuadrant(t0-Pi/8,f0+Pi/8)==(f,0))
      assert(c.getFaceQuadrant(t0+Pi/8,f0+Pi/8)==(f,1))
      assert(c.getFaceQuadrant(t0-Pi/8,(f0-Pi/8+2*Pi)%(2*Pi))==(f,2))
      assert(c.getFaceQuadrant(t0+Pi/8,(f0-Pi/8+2*Pi)%(2*Pi))==(f,3))
    }
    //upper face (4)
    assert(c.getFaceQuadrant(Pi/8,Pi/4)==(4,1))
    assert(c.getFaceQuadrant(Pi/8,Pi/4+Pi/2)==(4,0))
    assert(c.getFaceQuadrant(Pi/8,Pi/4+Pi)==(4,2))
    assert(c.getFaceQuadrant(Pi/8,Pi/4+3*Pi/2)==(4,3))

    //bottom face (5)
    assert(c.getFaceQuadrant(Pi-Pi/8,Pi/4)==(5,0))
    assert(c.getFaceQuadrant(Pi-Pi/8,Pi/4+Pi/2)==(5,1))
    assert(c.getFaceQuadrant(Pi-Pi/8,Pi/4+Pi)==(5,3))
    assert(c.getFaceQuadrant(Pi-Pi/8,Pi/4+3*Pi/2)==(5,2))

  }
 
  test("get Local index from theta,phi"){
    val ai=30.0
    val bi=40.0
    val (f,q,i,j)=c.getLocalIndex(Pi/2-toRadians(bi),toRadians(ai))
    assert(f==0 & q==0 & (i,j)==(0,1),s"\n alpha=$ai deg beta=$bi deg=> f=$f q=$q i=$i j=$j")
  }

  /*
  test("Angles to local"){
      val tet=List.tabulate(100){i=>i/100*Pi}
      val phi=List.tabulate(100){i=>i/100*2*Pi}

      for (t<-tet;p<-phi){
        val face=c.getFace(t,p)
        val (x,y)=c.ang2Local(face)(t,p)
        assert(abs(x)<1 & abs(y)<1,s"fail on f=$face")
      }
  }
   */

  /*
  test("Ang2pix over pixel centers"){
      for (ipix<-c.pixNums) {
        val Array(theta,phi)=c.pix2ang(ipix)
        val ipixback=c.ang2pix(theta,phi)
        val (face,i,j)=c.pix2coord(ipix)
        val (faceb,ib,jb)=c.pix2coord(ipixback)
        assert(ipix==ipixback,f"\nipix=$ipix face=$face i=$i j=$j thetat=$theta%f phi=$phi%f\npixback=$ipixback face=$faceb i=$ib j=$jb")
      }
    }

  test("Pixels max radius") {

    //theoretical values for square
    val Asq=4*Pi/(6*N*N)
    val Rmax=1.25*sqrt(Asq/2)

    val Ntot=1000000
    //random angles
    val angles=Seq.fill(Ntot)((acos(2*Random.nextDouble-1),2*Pi*Random.nextDouble))

    for ((t,f) <- angles) {
      val ipix=c.ang2pix(t,f)
      val Array(tc,fc)=c.pix2ang(ipix)
      val p=new Point3D(t,f)
      val cen=new Point3D(tc,fc)
      val r=cen.dist(p)
      assert(r<Rmax,s"\n ipix=$ipix theta=$t phi=$f r=$r")
    }
  }
   */
 

  test("Neighbours"){
    //theoretical values for square
    val Asq=4*Pi/(6*N*N)
    val Rsq=sqrt(Asq/2)
    val Rmin=Rsq
    val Rmax=1.1*Rsq

    
    for (ipix<-c.pixNums) {
      val id=c.pix2coord(ipix)
      val Array(tc,phic)=c.pix2ang(ipix)
      val pcen=new Point3D(tc,phic)

      val n=c.neighbours(ipix)
      for (in <- n) {
        val (f,i,j)=c.pix2coord(in)
        val ang=c.pix2ang(in)
        val p=new Point3D(ang(0),ang(1))
        val r=pcen.dist(p)
        assert(r>1.2*Rmin & r<2*Rmax,s"\n ipix=$ipix$id ang=($tc,$phic) voisins=$n : $pcen=pcen -> fail on $in=($f,$i,$j) angle=${ang(0)},${ang(1)} p=$p")
      }

    } //pixnum



  } //test


}
