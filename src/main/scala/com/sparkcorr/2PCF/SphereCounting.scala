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
import com.sparkcorr.Tiling.{SARSPix,HealpixGrid}




import java.util.Locale



//companion
object SphereCounting {
 
 //main
  def main(args:Array[String]):Unit= {
    Locale.setDefault(Locale.US)

    if (args.size!= 1){
      println("*****************************************")
      println(">>>> Usage: SphereCounting paramFile")
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
    val btype=params.get("bin_type","log")


    val binning=btype match {
      case "log" => new LogBinning(bmin,bmax,Nbins)
      case _ => throw new Exception("Invalid binning type: "+btype)
    }

    //PRINT
    println("td,tu,w,Nc,Npixc,Nj,Npixj")
   for ((b,w) <- binning.bin.zip(binning.binW)) {
     val (a,nc,nj)= til match {
       case "sarspix" => (6,SARSPix.pixRadiusLt(w/2),SARSPix.pixRadiusGt(b(1)/2))
       case "healpix" => (12,HealpixGrid.pixRadiusLt(w/2),HealpixGrid.pixRadiusGt(w/2))
       case _ => throw new Exception("Unknown tiling: "+til)
     }

     println(f"${b(0)}%.1f,${b(1)}%.1f,${w}%.1f,$nc%d,${a*nc*nc.toDouble/1e6},$nj%d,${a*nj*nj.toDouble/1e3}")
   }
   params.checkRemaining

  }

}


