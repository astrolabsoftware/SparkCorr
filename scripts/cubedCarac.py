from pylab import *
from tools import *

from pylab import *

#
a=1/sqrt(3.)

#for one face
N=int(sys.argv[1])

#equal angle
la=linspace(-pi/4,pi/4,N)
xx=a*tan(la)
x,y=meshgrid(xx,xx)

normalize=lambda p: p/sqrt(p.dot(p))

#nodes
nodes=[]
for i in range(N):
    for j in range(N):
        ipix=i*N+j
        p=array([x[i,j],y[i,j],a])
        p=normalize(p)
        nodes.append(p)

def dist(p1,p2):
    return sqrt(sum((p1-p2)**2))

def dist2(p1,p2):
    return sum((p1-p2)**2)

def xyz_to_thetaphi(v):
    x,y,z=v
    phi=np.arctan2(y,x) #[0,2pi]
    t=np.arccos(z) #[0,pi]
    return [t,phi]


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
    p=sqrt(p2)
    q=sqrt(q2)
    e[i,j]=(p-q)/(p+q)
    cen=(A+B+C+D)/4.
    CA=dist(cen,A)
    CB=dist(cen,B)
    CC=dist(cen,C)
    CD=dist(cen,B)
    ri=array([CA,CB,CC,CD])
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


Aexp=4*pi/6/(N)**2
Rsq=sqrt(Aexp/2)
Rsqin=Rsq/sqrt(2)

imshowXY(arange(N-1),arange(N-1),Air/Aexp,vmin=0.85,vmax=1.15)
title("area")
yticks(gca().get_xticks())  
ylim(gca().get_xlim())
savefig("cubed_area2.pdf")

#imshowXY(arange(N-1),arange(N-1),e,vmin=0,vmax=1)
imshowXY(arange(N-1),arange(N-1),abs(e)vmin=0,vmax=.30)
title("ellipticity")
yticks(gca().get_xticks())  
ylim(gca().get_xlim())
savefig("cubed_e2.pdf")


imshowXY(arange(N-1),arange(N-1),Rmax/Rsq,vmin=0.85,vmax=1.35)
title("outer radius")
yticks(gca().get_xticks())  
ylim(gca().get_xlim())
savefig("cubed_rout2.pdf")

imshowXY(arange(N-1),arange(N-1),Rint/Rsqin,vmin=0.7,vmax=1.1)
title("inner radius")
yticks(gca().get_xticks())  
ylim(gca().get_xlim())
savefig("cubed_rin2.pdf")


#histo R
figure()
axes()

range=[0.6,1.5]
#hist(Rin/Rsqin,bins=80,range=range,label=r"$R_{in}$")
hist(Rint.flat/Rsqin,bins=80,range=range,label="inner")
hist(Rmax.flat/Rsq,color='red',bins=80,alpha=0.7,range=range,label="outer")

xlabel(r"radius")
legend()
xlim(range)
#xticks(linspace(0.7,1.5,9))
semilogy()
show()
#savefig("cubed_radius.pdf")

print("Rmin={:.3f}".format(amin(Rint.flat/Rsqin)))
print("Rmax={:.3f}".format(amax(Rmax.flat/Rsq)))
