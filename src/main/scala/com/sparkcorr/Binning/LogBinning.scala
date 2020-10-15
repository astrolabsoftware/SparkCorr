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
class LogBinning(val start:Double,val end:Double,val Nbins:Int) extends Binning {

val logbW:Double = (log(end)-log(start))/Nbins
val logt=Array.tabulate(Nbins+1)(i=>log(start)+i*logbW)

val bin=logt.map(exp).sliding(2).toArray

}

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
    //println(binning.bin.deep.mkString("\n"))

    for (i<-0 until binning.bin.size) 
      println(f"[${binning.bin(i)(0)}%.2f, ${binning.bin(i)(1)}%.2f] w=${binning.binW(i)}%.2f")
      
  }


}

