from pylab import *
import pandas

x=linspace(0.8,1.5,100)
hcube=loadtxt("hcubed.txt")
hcobe=loadtxt("hcobe.txt")
hsars=loadtxt("hsars.txt")


hp=zeros_like(x)
df=pandas.read_csv("hp128.csv") 

fill(df['loc'],df['count']/sum(df['count']), label="Healpix",lw=3)
fill(x,hcube/sum(hcube),label="CubedSphere",lw=3,alpha=0.7)
fill(x,hcobe/sum(hcobe),label="COBE",lw=3,alpha=0.7)
fill(x,hsars/sum(hsars),label="SARSPix",lw=3,alpha=0.7)


ylim(1e-3)
xlim(0.9,1.45)
xlabel("pixel radius")
legend()
show()
