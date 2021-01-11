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
/**
  * Abstract class to encapulate pixelization properties
  * 
  *  @author Stephane Plaszczynski
  */
abstract class PixProps {

  /** number of pixels
    */
  def Npix(nbase:Int):Long
  /**
    *  Nbase below which all pixel radii are greater than R
    */
  def pixRadiusGt(R:Double):Int
  /** 
    * Nbase above which all pixels have radii lower than R
    */
  def pixRadiusLt(R:Double):Int

}
