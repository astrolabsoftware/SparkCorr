from pyspark.sql import SparkSession
from pyspark import StorageLevel

from df_tools import * 
from histfile import *
from tools import *
from pyspark.sql import functions as F
import sys

method=sys.argv[1]
nside=int(sys.argv[2])
tit=""
if (method=="hp"):
    tit="Healpix (nside={})".format(nside)
    Npix=12*nside**2
else :
    tit="CubedSphere (nside={})".format(nside)
    Npix=6*nside**2


Rsq=rad2arcmin(sqrt(4*pi/Npix/2))

spark = SparkSession.builder.getOrCreate()

#df=spark.read.parquet("hp_nside{}.parquet".format(nside))
df=spark.read.parquet(method+"_nside{}.parquet".format(nside))

df=df.withColumn("dx",F.degrees(F.sin((df["theta"]+df["theta_c"])/2)*(df["phi"]-df["phi_c"]))*60)
df=df.withColumn("dy",F.degrees(df["theta"]-df["theta_c"])*60)
#df=df.withColumn("r",F.hypot(df["dx"],df["dy"]))

df=df.withColumn("x",F.sin(df["theta"])*F.cos(df["phi"])).withColumn("y",F.sin(df["theta"])*F.sin(df["phi"])).withColumn("z",F.cos(df["theta"])).drop("theta","phi")

df=df.withColumn("xc",F.sin(df["theta_c"])*F.cos(df["phi_c"])).withColumn("yc",F.sin(df["theta_c"])*F.sin(df["phi_c"])).withColumn("zc",F.cos(df["theta_c"])).drop("theta_c","phi_c")

df=df.withColumn("R",F.degrees(F.hypot(df.x-df.xc,F.hypot(df.y-df.yc,df.z-df.zc)))*60).drop("x","y","z","xc","yc","zc")

#distance in arcmin
#df=df.withColumn("angdist",F.degrees(2*F.asin(df.rr/2))*60)
df=df.withColumn("Rscaled",df.R/Rsq)

df.cache().count()

print("Rsq={} arcmin".format(Rsq))
maxr=df.select(F.max(df.R)).take(1)[0][0]
print("max radius={} arcmin".format(maxr))

#2d
x,y,m=df_histplot2(df,"dx","dy",bounds=[[-maxr,maxr],[-maxr,maxr]],Nbin1=200,Nbin2=200)        
#imshowXY(x,y,m)
xlabel(r"$\Delta x\quad [arcmin]$")
ylabel(r"$\Delta y\quad [arcmin]$")
title(tit)
savefig(method+"_nside{}_2d.png".format(nside))

#log
figure()
imshowXY(x,y,log10(1+m))   
xlabel(r"$\Delta x\quad [arcmin]$")
ylabel(r"$\Delta y\quad [arcmin]$")
title(tit+" [log]")
savefig(method+"_nside{}_2dlog.png".format(nside))


#Rmax by pixel                                              
dfpix=df.groupBy("ipix").agg(F.max(df["R"]))
df_histplot(dfpix,dfpix.columns[1],doStat=True)
xlabel("pixel radius [arcmin]")
title(tit)
savefig(method+"_nside{}_1d.png".format(nside))
