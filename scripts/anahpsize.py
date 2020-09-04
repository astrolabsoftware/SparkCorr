from pyspark.sql import SparkSession
from pyspark import StorageLevel

from df_tools import * 
from histfile import *
from tools import *
from pyspark.sql import functions as F
import sys

nside=int(sys.argv[1])

Npix=12*nside**2
Rsq=sqrt(4*pi/Npix/2)

Nf=int(sqrt(pi/3)/Rsq)

print("nside={} Rsq={:.2f} arcmin Nf={}".format(nside,rad2arcmin(Rsq),Nf))
 
spark = SparkSession.builder.getOrCreate()

df=spark.read.parquet("hp_nside{}.parquet".format(nside))

df=df.withColumn("dx",F.degrees(F.sin((df["theta"]+df["theta_c"])/2)*(df["phi"]-df["phi_c"]))*60)
df=df.withColumn("dy",F.degrees(df["theta"]-df["theta_c"])*60)
#df=df.withColumn("r",F.hypot(df["dx"],df["dy"]))

df=df.withColumn("x",F.sin(df["theta"])*F.cos(df["phi"])).withColumn("y",F.sin(df["theta"])*F.sin(df["phi"])).withColumn("z",F.cos(df["theta"])).drop("theta","phi")

df=df.withColumn("xc",F.sin(df["theta_c"])*F.cos(df["phi_c"])).withColumn("yc",F.sin(df["theta_c"])*F.sin(df["phi_c"])).withColumn("zc",F.cos(df["theta_c"])).drop("theta_c","phi_c")

df=df.withColumn("rr",F.hypot(df.x-df.xc,F.hypot(df.y-df.yc,df.z-df.zc))).drop("x","y","z","xc","yc","zc")

#distance in arcmin
df=df.withColumn("R",F.degrees(2*F.asin(df.rr/2))*60)
df=df.withColumn("R/Rsq",df.R/Rsq)


df.cache().count()

maxr=df.select(F.max(df.R)).take(1)[0][0]                                                 

p=df_histplot(df,"R")
xlabel(r"R [arcmin]")
text(0.8,0.8,r"$\theta_u={:.2f}^\prime$".format(maxr),transform=gca().transAxes) 
title("nside={}".format(nside))
#savefig("nside{}_1d.png".format(nside))


x,y,m=df_histplot2(df,"dx","dy",bounds=[[-maxr,maxr],[-maxr,maxr]],Nbin1=200,Nbin2=200)        
clf()
#imshowXY(x,y,log10(1+m))   
imshowXY(x,y,m)
title("nside={}".format(nside))
#savefig("nside{}_2d.png".format(nside))
 
#Rmax by pixel                                               
dfpix=df.groupBy("ipix").agg(F.max(df["R"]))
df_histplot(dfpix,dfpix.columns[1],doStat=True)
