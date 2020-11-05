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
package com.sparkcorr.`2PCF`

import com.sparkcorr.Binning.{LogBinning}
import com.sparkcorr.IO.{ParamFile}
import com.sparkcorr.Tiling.{SARSPix,HealpixGrid,CubedSphere}

import scala.math.{sqrt}



import java.util.Locale



//companion
object BinSetup {
 
 //main
  def main(args:Array[String]):Unit= {
    Locale.setDefault(Locale.US)

    if (args.size!= 1){
      println("*****************************************")
      println(">>>> Usage: BinSetup paramFile")
      println("*****************************************")
      return
    }
    require(scala.reflect.io.File(args(0)).exists)

    val params=new ParamFile(args(0))
    val til=params.get("tiling","SARSPix").toLowerCase 

    //binning
    val Nbins:Int=params.get("Nbins",0)
    val bmin:Double=params.get("bin_min",0.0)
    val bmax:Double=params.get("bin_max",0.0)
    val btype:String=params.get("bin_type","log")


    val binning=btype match {
      case "log" => new LogBinning(bmin,bmax,Nbins)
      case _ => throw new Exception("Invalid binning type: "+btype)
    }

    //PRINT
    val sep=List.tabulate(75)(i=>"-").reduce(_+_) 
    println(sep)
    println("id\ttd\ttu\tw\tNd\tNpixD(M)\tNj\tNpixJ(k)")
    println(sep)
   for (((b,w),id) <- binning.bins.zip(binning.binW).zipWithIndex) {
     val a = til match {
       case "sarspix" => (6,SARSPix.pixRadiusLt(w/2),SARSPix.pixRadiusGt(b(1)/sqrt(2)))
       case "cubedsphere" => (6,CubedSphere.pixRadiusLt(w/2),CubedSphere.pixRadiusGt(b(1)/sqrt(2)))
       case "healpix" => (12,HealpixGrid.pixRadiusLt(w/2),HealpixGrid.pixRadiusGt(b(1)/sqrt(2)))
       case _ => throw new Exception("Unknown tiling: "+til)
     }
     val f=a._1
     val Nd=a._2
     val Nj=a._3

     println(f"$id\t${b(0)}%.1f\t${b(1)}%.1f\t${w}%.1f\t$Nd%d\t${f*Nd*Nd.toDouble/1e6}\t$Nj%d\t${f*Nj*Nj.toDouble/1e3}")
   }
    println(sep)
   params.checkRemaining

  }

}


