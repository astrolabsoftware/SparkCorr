//spark-shell --jars jhealpix.jar -I hpgrid.scala

import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.{functions=>F}
import org.apache.spark.sql.types._

import java.util.Locale
import scala.math.{Pi}
Locale.setDefault(Locale.US)


//healpix
val nside:Int=64
val N:Long=10000000


val grid = HealpixGrid(new HealpixBase(nside, NESTED), new ExtPointing)
def Ang2Pix=spark.udf.register("Ang2Pix",(theta:Double,phi:Double)=>grid.index(theta,phi))
val Pix2Ang=spark.udf.register("PixAang",(ipix:Long)=> grid.pix2ang(ipix))


var df=spark.range(0,N).withColumn("theta",F.acos(F.rand*2-1.0)).withColumn("phi",F.rand*2*Pi).drop("id")

println("#parts="+df.rdd.getNumPartitions)

//add pixelnum
df=df.withColumn("ipix",Ang2Pix($"theta",$"phi"))

//add pixel center
df=df.withColumn("ptg",Pix2Ang($"ipix")).withColumn("theta_c",$"ptg"(0)).withColumn("phi_c",$"ptg"(1)).drop("ptg")


df.write.mode("overwrite").parquet(s"nside${nside}.parquet")
