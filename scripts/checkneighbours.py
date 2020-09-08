
from pylab import *
from scipy import r_

f,x,y,z=loadtxt("centers.txt",unpack=True)  

f,i,j,xn,yn,zn=loadtxt("neighbours.txt",unpack=True)

xx=r_[x,xn]
yy=r_[y,yn]
zz=r_[z,zn]

cols=zeros_like(xx)*256

strs = ["grey" for v in x]
strs+=["blue"]
strs+=["red" for v in xn[1:]]

cols[len(x)::]=1

fig = figure(figsize=(10,10))
ax = fig.add_subplot(111, projection='3d')

ax.scatter(xx,yy,zz,c=strs)

show()
