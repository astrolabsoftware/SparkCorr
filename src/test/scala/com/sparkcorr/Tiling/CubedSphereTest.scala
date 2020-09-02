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
import org.scalatest.{BeforeAndAfterAll, FunSuite}
import scala.math.{Pi}

/**
  * Test class for CubedSphere
  */
class CubedSphereTest extends FunSuite with BeforeAndAfterAll {

  var c: CubedSphere = _
  val N:Int= 10

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    c= new CubedSphere(N)
  }

  test("cubed sphere creation") {
    val cube=new CubedSphere(10)
    assert(cube.N==N)

  }

  test("valid pixel numbers"){
    for (ipix<-c.pixNums) {
      assert(c.isValidPix(ipix)==true)
    }

  }

  test("pixnum to coord") {
    for (ipix<-c.pixNums) {
      val (f,i,j)=c.pix2coord(ipix)
      assert(c.coord2pix(f,i,j)==ipix)
    }
  }

 test("coord to pixnum") {
    for (f<-0 to 5; i<-0 until N; j<- 0 until N) {
      val ipix=c.coord2pix(f,i,j)
      assert(c.pix2coord(ipix)==(f,i,j))
    }
  }

  test("face from angles") {
    val tet=List(Pi/2,Pi/2,Pi/2,Pi/2,0,Pi)
    val phi=List(0.0,Pi/2,Pi,3*Pi/2,0,0)

    for (((t:Double,p:Double),f:Int) <- tet.zip(phi).zipWithIndex) {
      val face=c.getFace(t,p)
      assert(face==f)
    }


  }







}
