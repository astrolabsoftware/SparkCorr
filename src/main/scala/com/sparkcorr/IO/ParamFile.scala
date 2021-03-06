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

  require(scala.reflect.io.File(f).exists,"\n file "+f+" does not exist")

  var m=ParamFile.parsefile(f)

  def contains(key:String) = m.contains(key)
  override def toString = m.mkString("\n")

  //with Option
  def get[T](key:String,conv: String=>T):Option[T]= {
    if (m.contains(key)) {
      val s=m(key)
      println("Parsing "+f+": --> "+key+"="+s)
      m=m.-(key)
      Some(conv(s))
    }
    else {
      println("Parsing "+f+": --> WARNING: "+key+" not found") 
      None
    }
  }

  //specifying default value
  def get(key:String,dflt:String):String = get[String](key,_.toString).getOrElse(dflt)
  def get(key:String,dflt:Short):Short = get[Short](key,_.toShort).getOrElse(dflt)
  def get(key:String,dflt:Int):Int = get[Int](key,_.toInt).getOrElse(dflt)
  def get(key:String,dflt:Long):Long = get[Long](key,_.toLong).getOrElse(dflt)
  def get(key:String,dflt:Float):Float = get[Float](key,_.toFloat).getOrElse(dflt)
  def get(key:String,dflt:Double):Double = get[Double](key,_.toDouble).getOrElse(dflt)

  def checkRemaining():Unit={
    if (! m.isEmpty) {
      println("WARNING parameters not used:")
      for ((k,v)<-m) println("--->"+k+"="+v)
    }
  }

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
      if (m.contains(a(0)))
        throw new Exception("parameter "+a(0)+ " already defined")
      m(a(0))=a(1)
    }
    m
  }

  def main(args:Array[String]):Unit= {

    require(args.size==1)
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

