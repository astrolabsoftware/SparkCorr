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
import com.sparkcorr.Geometry.{Point3D,arr2}

import org.scalatest.{BeforeAndAfter, FunSuite}
import scala.math.{Pi,abs,acos,asin,sqrt,toRadians,toDegrees}
import scala.util.Random
import java.util.Locale

/**
  * Test class for SARSPix
  */
class SARSPixTest extends FunSuite with BeforeAndAfter {

  var c: SARSPix = _
  val N:Int= 10

  before {
    Locale.setDefault(Locale.US)
    //println(s"creating SARSPix($N)")
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
    for (I <- 0 until c.Nbase) {
      for (J <- 0 until c.Nbase) {
        val (q,i,j)=c.face2localBinIndex(I,J)
        assert(c.local2faceBinIndex(q,i,j)==(I,J))
      }
    }
  }


  test("ang2pix on edges"){
    val tet=List(2.0260583888217623)
    val phi=List(2.356194490192345)

    for (t<-tet;p<-phi){
      val ipix=c.ang2pix(t,p)
      assert(ipix>=0 & ipix<c.maxIndex)
    }

  }




  test("test all indices") {
     for (ipix<-c.pixNums) {
       val (f,ii,jj)=c.pix2coord(ipix)
       val (q,i,j)=c.face2localBinIndex(ii,jj)

       val (ib,jb)=c.local2faceBinIndex(q,i,j)
       assert(ib==ii & jb==jj)

       assert(c.coord2pix(f,ib,jb)==ipix)

     }
  
}

  test("Faces and quadrants") {
 
    //azimuthal
    for (f <- 0 to 3) {
      val f0=f*Pi/2
      val t0=Pi/2
      assert(c.getFaceQuadrant(new Point3D(t0-Pi/8,f0+Pi/8))==(f,0))
      assert(c.getFaceQuadrant(new Point3D(t0+Pi/8,f0+Pi/8))==(f,1))
      assert(c.getFaceQuadrant(new Point3D(t0-Pi/8,(f0-Pi/8+2*Pi)%(2*Pi)))==(f,2))
      assert(c.getFaceQuadrant(new Point3D(t0+Pi/8,(f0-Pi/8+2*Pi)%(2*Pi)))==(f,3))
    }
    //upper face (4)
    assert(c.getFaceQuadrant(new Point3D(Pi/8,Pi/4))==(4,1))
    assert(c.getFaceQuadrant(new Point3D(Pi/8,Pi/4+Pi/2))==(4,0))
    assert(c.getFaceQuadrant(new Point3D(Pi/8,Pi/4+Pi))==(4,2))
    assert(c.getFaceQuadrant(new Point3D(Pi/8,Pi/4+3*Pi/2))==(4,3))

    //bottom face (5)
    assert(c.getFaceQuadrant(new Point3D(Pi-Pi/8,Pi/4))==(5,0))
    assert(c.getFaceQuadrant(new Point3D(Pi-Pi/8,Pi/4+Pi/2))==(5,1))
    assert(c.getFaceQuadrant(new Point3D(Pi-Pi/8,Pi/4+Pi))==(5,3))
    assert(c.getFaceQuadrant(new Point3D(Pi-Pi/8,Pi/4+3*Pi/2))==(5,2))

  }


  test("Pix2Ang+Ang2pix "){
      for (ipix<-c.pixNums) {

        val (f,ii,jj)=c.pix2coord(ipix)
        //coords
        val (q,i,j)=c.face2localBinIndex(ii,jj)
        //pos
        val Array(theta,phi)=c.pix2ang(ipix)

        //ang2pix
        val p=new Point3D(theta,phi)

        val (fb,qb,ib,jb)=c.getLocalIndex(p)

        val ipixb=c.ang2pix(theta,phi)

        assert(fb==f & qb==q & ib==i &jb==j,s"\ninput: ai=${toDegrees(phi)} bij=${toDegrees(Pi/2-theta)} ipix=$ipix -> face=$f($ii,$jj) -> q=$q($i,$j)  \noutput: ipix=$ipixb f=$fb,q=$qb($ib,$jb)")
      }
    
  }//test
 

  test("Pixels max radius") {

    //theoretical values for square
    val Asq=4*Pi/(6*N*N)
    val Rsq=sqrt(Asq/2)
    val Rmax=1.1*Rsq

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
