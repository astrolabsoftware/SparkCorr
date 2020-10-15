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
package com.sparkcorr.IO

import org.apache.spark.sql.{SparkSession}
import org.apache.spark.SparkContext
import org.apache.log4j.{Level, Logger}


import scala.io.Source
import collection.mutable.Map

class ParamFile(f:String) {
  val m=ParamFile.parsefile(f)

  def contains(key:String) = m.contains(key)
  override def toString = m.mkString("\n")

  def getType[T](key:String,dflt:T,conv: String=>T):T= if (m.contains(key)) conv(m(key)) else dflt

  def get(key:String,dflt:String):String = m.getOrElse(key,dflt).toString
  def get(key:String,dflt:Short):Short = getType[Short](key,dflt,_.toShort)
  def get(key:String,dflt:Int):Int = getType[Int](key,dflt,_.toInt)
  def get(key:String,dflt:Long):Long = getType[Long](key,dflt,_.toLong)
  def get(key:String,dflt:Float):Float = getType[Float](key,dflt,_.toFloat)
  def get(key:String,dflt:Double):Double = getType[Double](key,dflt,_.toDouble)

}

//companion
object ParamFile {

  def parsefile(f:String):Map[String,String]={
    val m=Map.empty[String,String]
    for (
      line <-Source.fromFile(f).getLines()
      if !line.startsWith("#")
      if (line.count(_ == '=') == 1)
        ) {
      val a=line.split("=").map(_.trim)
      m(a(0))=a(1)
    }
    m
  }

  def main(args:Array[String]):Unit= {

    require(args.size==1 && scala.reflect.io.File(args(0)).exists)
    val file=args(0)

   // Set verbosity
    Logger.getLogger("org").setLevel(Level.WARN)
    Logger.getLogger("akka").setLevel(Level.WARN)

    /*
    val spark = SparkSession
      .builder()
      .appName("random_rgg")
      .getOrCreate()

    val sc: SparkContext = spark.sparkContext
     */

    println(s"reading $file")
    val params=new ParamFile(args(0))
    println(params)
    val a=params.get("a",0L)
    val b=params.get("b","nice")
    val c=params.get("c",1.0)
    println(s"a=$a b=$b c=$c")



  }

}

