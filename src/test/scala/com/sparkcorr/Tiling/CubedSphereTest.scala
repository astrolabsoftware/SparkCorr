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
import scala.math.{Pi,abs,acos,sqrt}
import scala.util.Random
import java.util.Locale

/**
  * Test class for CubedSphere
  */
class CubedSphereTest extends FunSuite with BeforeAndAfter {

  var c: CubedSphere = _
  val N:Int= 10

  before {
    Locale.setDefault(Locale.US)
    c= new CubedSphere(N)
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


  test("Face from angles") {
    val tet=List(Pi/2,Pi/2,Pi/2,Pi/2,0,Pi)
    val phi=List(0.0,Pi/2,Pi,3*Pi/2,0,0)

    for (((t:Double,p:Double),f:Int) <- tet.zip(phi).zipWithIndex) {
      val face=c.getFace(t,p)
      assert(face==f)
    }
  }


  test("Angles to local"){
      val tet=List.tabulate(100){i=>i/100*Pi}
      val phi=List.tabulate(100){i=>i/100*2*Pi}

      for (t<-tet;p<-phi){
        val face=c.getFace(t,p)
        val (x,y)=CubedSphere.ang2Local(face)(t,p)
        assert(abs(x)<1 & abs(y)<1,s"fail on f=$face")
      }
  }

  test("ang2pix on edges"){
    val tet=List(2.0260583888217623)
    val phi=List(2.356194490192345)

    for (t<-tet;p<-phi){
      val face:Int=c.getFace(t,p)
      assert(face>=0 & face<=5,s"\n fail face=$face")
      val (x,y)=CubedSphere.ang2Local(face)(t,p)
      val (i,j)=c.ang2LocalIndex(face,t,p)
      assert((i>= 0) & (i< c.N) & (j>=0) & (j< c.N) ,s"\n fail f=$face i=$i j=$j")
    }

  }


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
      assert(c.getFace(t,f)==c.getFace(p))
    }
  }

  test("Neighbours"){
    //theoretical values for square
    val Asq=4*Pi/(6*N*N)
    val Rmin=0.85*sqrt(Asq/2)
    val Rmax=1.26*sqrt(Asq/2)

    
    for (ipix<-c.pixNums) {
      //center
      val Array(tc,fc)=c.pix2ang(ipix)
      val cen=new Point3D(tc,fc)
      val (face,ic,jc)=c.pix2coord(ipix)
    
      val neighb=c.neighbours(ipix)
      for (n<-neighb) {
        assert(c.isValidPix(n),s"\n $n=(($face,$ic,$jc)")
        val Array(t,f)=c.pix2ang(n)
        val p=new Point3D(t,f)
        val r=cen.dist(p)
        val (ff,i,j)=c.pix2coord(n)
        assert(r>1.2*Rmin,s"\n($face,$ic,$jc) vs ($ff,$i,$j) : r=$r Rmin=$Rmin")
        assert(r<2*Rmax,s"\n($face,$ic,$jc) vs ($ff,$i,$j) : r=$r Rmax=${Rmax}")
      }
    } //pixnum
  } //test


}
