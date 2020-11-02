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
Rmin=zeros((N-1,N-1))
Rint=zeros((N-1,N-1))
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
    CA=dist(cen,A)
    CB=dist(cen,B)
    CC=dist(cen,C)
    CD=dist(cen,B)
    ri=array([CA,CB,CC,CD])
    #line=" ".join([str(x) for x in cen])+"\n"
    #ff.write(line)
    Rmax[i,j]=amax(ri)
    Rmin[i,j]=amin(ri)

    a=CA
    b=CB
    c=sqrt(a2)
    p=(a+b+c)/2
    h1=2*sqrt(p*(p-a)*(p-b)*(p-c))/c

    a=CB
    b=CC
    c=sqrt(b2)
    p=(a+b+c)/2
    h2=2*sqrt(p*(p-a)*(p-b)*(p-c))/c
 
    a=CC
    b=CD
    c=sqrt(c2)
    p=(a+b+c)/2
    h3=2*sqrt(p*(p-a)*(p-b)*(p-c))/c

    a=CD
    b=CA
    c=sqrt(d2)
    p=(a+b+c)/2
    h4=2*sqrt(p*(p-a)*(p-b)*(p-c))/c
   
    h=array([h1,h2,h3,h4])
    Rint[i,j]=amin(h)



##
ff.close()


Aexp=4*pi/6/(N-1)**2
Rsq=sqrt(Aexp/2)
Rsqin=Rsq/sqrt(2)

imshowXY(arange(N-1),arange(N-1),Air/Aexp,vmin=0.85,vmax=1.15)
title("area")

imshowXY(arange(N-1),arange(N-1),abs(e-1),vmin=0,vmax=0.8)
title("ellipticity")

imshowXY(arange(N-1),arange(N-1),Rmax/Rsq,vmin=0.85,vmax=1.35)
title("Rout")

imshowXY(arange(N-1),arange(N-1),Rint/Rsqin,vmin=0.7,vmax=1.1)
title("Rin")

#histo R

figure()
Rmin=Rmin.flatten()
Rmax=Rmax.flatten()
Rin=Rmin*Rmax/sqrt(Rmin**2+Rmax**2)

#hist(Rmin,bins=80,range=[0.7,1.5])
range=[0.7,1.5]
#hist(Rin/Rsqin,bins=80,range=range,label=r"$R_{in}$")
hist(Rint.flat/Rsqin,bins=80,range=range,label=r"$R_{in}$")
hist(Rmax/Rsq,color='red',bins=80,alpha=0.7,range=range,label=r"$R_{out}$")

xlabel(r"$R/R_{sq}$")
legend()
xlim(range)
#xticks(linspace(0.7,1.5,9))
#semilogy()
show()
savefig("sars_rmax1.png")

