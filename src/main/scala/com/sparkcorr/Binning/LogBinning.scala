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
package com.sparkcorr.Binning

import scala.math.{log,exp}
import java.util.Locale

//class
class LogBinning(val start:Double,val end:Double,val Nbins:Int) 
    extends Binning(Array.tabulate(Nbins+1)(i=>log(start)+i*(log(end)-log(start))/Nbins).map(exp))


//companion
object LogBinning {
 
 //factory method      
  def apply(start:Double,end:Double,Nbins:Int)=new LogBinning(start,end,Nbins)

 //main
  def main(args:Array[String]):Unit= {
    Locale.setDefault(Locale.US)

    if (args.size!= 3){
      println("*****************************************")
      println(">>>> Usage: LogBinning start end Nbins")
      println("*****************************************")
      return
    }
    val binning=LogBinning(args(0).toDouble,args(1).toDouble,args(2).toInt)
    //println(binning)
    for ((b,w) <- binning.bins.zip(binning.binW))
      println(f"[${b(0)}%6.2f,${b(1)}%6.2f] w=${w}%.2f")
      
  }


}

