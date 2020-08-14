name := "SparkCorr"
version := "1.0"
scalaVersion := "2.11.12"

organization := "com.github.astrolabsoftware"

version := "4.1"

//mainClass in Compile := Some("com.jec.rpdbscan.Demo")

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "2.4.5" % "provided",
  "org.apache.spark" %% "spark-sql" % "2.4.5" % "provided",
  "com.github.scopt" % "scopt_2.11" % "4.0.0-RC2"
)

licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))
