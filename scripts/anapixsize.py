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
elif (method=="cs"):
    tit="CubedSphere (nside={})".format(nside)
    Npix=6*nside**2
else :
    tit="SARSPix (nside={})".format(nside)
    Npix=6*nside**2

A=4*pi/Npix
Rsq=sqrt(A/2)

print("Rsq={} arcmin".format(rad2arcmin(Rsq)))

spark = SparkSession.builder.getOrCreate()

fn=method+"_nside{}.parquet".format(nside)
print("reading: "+fn)

df=spark.read.parquet(fn)

df=df.withColumn("dx",F.sin((df["theta"]+df["theta_c"])/2)*(df["phi"]-df["phi_c"])/Rsq)

df=df.withColumn("dy",(df["theta"]-df["theta_c"])/Rsq)

#df=df.drop("theta","phi","theta_c","phi_c")
#df=df.withColumn("R",F.hypot(df["dx"],df["dy"]))

df=df.withColumn("x",F.sin(df["theta"])*F.cos(df["phi"])).withColumn("y",F.sin(df["theta"])*F.sin(df["phi"])).withColumn("z",F.cos(df["theta"])).drop("theta","phi")
df=df.withColumn("xc",F.sin(df["theta_c"])*F.cos(df["phi_c"])).withColumn("yc",F.sin(df["theta_c"])*F.sin(df["phi_c"])).withColumn("zc",F.cos(df["theta_c"])).drop("theta_c","phi_c")
df=df.withColumn("R",F.hypot(df.x-df.xc,F.hypot(df.y-df.yc,df.z-df.zc))).drop("x","y","z","xc","yc","zc")
#df=df.withColumn("R",F.degrees(df['Rad'])*60/Rsq)

#df=df.withColumn("RR",F.hypot(df.dx,df.dy))

#angular distance in arcmin
#df=df.withColumn("angdist",F.degrees(2*F.asin(df.rr/2))*60)

df.cache().count()

print("Rsq={} arcmin".format(Rsq))
#maxr=df.select(F.max(df.R)).take(1)[0][0]
#print("max radius={} arcmin".format(maxr))

#2d
x,y,m=df_histplot2(df,"dx","dy",bounds=[[-1.5,1.5],[-1.5,1.5]],Nbin1=200,Nbin2=200)        
#imshowXY(x,y,m)
xlabel(r"$\Delta x$")
ylabel(r"$\Delta y$")
title(tit)
savefig(method+"_nside{}_2d.png".format(nside))

#log
imshowXY(x,y,log10(1+m))   
xlabel(r"$\Delta x$")
ylabel(r"$\Delta y$")
title(tit+" [log]")
savefig(method+"_nside{}_2dlog.png".format(nside))


#Rmax by pixel         
dfpix=df.groupBy("ipix").agg(F.max(df["R"]))
R=dfpix.toPandas()

Rmax=R[dfpix.columns[1]].values
Rmin=A/2./Rmax
Rin=Rmin*Rmax/sqrt(Rmin**2+Rmax**2)


Rsqin=Rsq/sqrt(2)
#hist(Rmin,bins=80,range=[0.7,1.5])
figure()
range=[0.7,1.5]
hist(Rin/Rsqin,bins=80,range=range,label=r"$R_{in}$")
hist(Rmax/Rsq,color='red',bins=80,alpha=0.7,range=range,label=r"$R_{out}$")
xlabel(r"$R/R_{sq}$")
legend()
xlim(range)
xticks(linspace(0.8,1.5,8))
#semilogy()
show()


#area
#dfc=df.groupBy("ipix").count()
#h,s=df_histplot(dfc,"count")
