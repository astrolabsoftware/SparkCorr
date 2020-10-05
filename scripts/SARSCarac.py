from pylab import *
from tools import *

assert len(sys.argv)==2
fn=sys.argv[1]

#read file
print("reading "+fn)

#nodes
nodes=loadtxt(fn)

N=int(sqrt(nodes.shape[0]))

def dist(p1,p2):
    return sqrt(sum((p1-p2)**2))

def dist2(p1,p2):
    return sum((p1-p2)**2)

#print("writing centers.txt")
ff=open("centers.txt",'w')

#cells
Air=zeros((N-1,N-1))
e=zeros((N-1,N-1))
Rmax=zeros((N-1,N-1))
for ip in range(N*N):
    i,j=ip//N,ip%N
    #skip borders
    if (i+1)%N==0 or (j+1)%N==0:
        continue
    A=nodes[ip]
    B=nodes[ip+1]
    C=nodes[ip+N+1]
    D=nodes[ip+N]

    p2=dist2(A,C)
    q2=dist2(B,D)

    a2=dist2(A,B)
    b2=dist2(B,C)
    c2=dist2(C,D)
    d2=dist2(D,A)

    Air[i,j]=sqrt(4*p2*q2-(b2+d2-a2-c2)**2)/4
#losange
    #A[i,j]=p*q/2
    e[i,j]=sqrt(p2/q2)
    cen=(A+B+C+D)/4.
    cen=cen/sqrt(sum(cen**2))
    line=" ".join([str(x) for x in cen])+"\n"
    #ff.write(line)
    ri=array([dist(cen,A),dist(cen,B),dist(cen,C),dist(cen,B)])
    Rmax[i,j]=amax(ri)

##
ff.close()


Aexp=4*pi/6/(N-1)**2
Rexp=sqrt(Aexp/2)

imshowXY(arange(N-1),arange(N-1),Air/Aexp,vmin=0.85,vmax=1.15)
title("area")

imshowXY(arange(N-1),arange(N-1),abs(e-1),vmin=0,vmax=0.8)
title("ellipticity")

imshowXY(arange(N-1),arange(N-1),Rmax/Rexp,vmin=0.85,vmax=1.35)
title("radius")

#histo R
figure()
hist_plot(Rmax.flatten()/Rexp,range=[0.8,1.5],bins=100,histtype='bar')

show()
