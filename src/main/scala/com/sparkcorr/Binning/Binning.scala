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

//loc=bin locations : there are Nbins+1 values
class Binning(val loc:Array[Double]){

  //intervals
  val bins:Array[Array[Double]]=loc.sliding(2).toArray
  //width
  val binW:Array[Double]=bins.map(b=>b(1)-b(0))

  override def toString: String =bins.deep.mkString("\n")

}
