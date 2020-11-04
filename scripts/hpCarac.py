from pylab import *
from tools import *
from healpy import *

from pylab import *

nside=int(sys.argv[1])
Npix=nside2npix(nside)

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
Air=zeros(Npix)
e=zeros(Npix)
Rmax=zeros(Npix)
Rmin=zeros(Npix)
Rint=zeros(Npix)

nodes=boundaries(nside,arange(Npix),1)

for ip in arange(Npix):
    
    A,B,C,D=nodes[ip].T 
    
    p2=dist2(A,C)
    q2=dist2(B,D)

    a2=dist2(A,B)
    b2=dist2(B,C)
    c2=dist2(C,D)
    d2=dist2(D,A)

    Air[ip]=sqrt(4*p2*q2-(b2+d2-a2-c2)**2)/4
#losange
    #A[i,j]=p*q/2
    e[ip]=sqrt(p2/q2)

    cen=pix2vec(nside,ip)

    CA=dist(cen,A)
    CB=dist(cen,B)
    CC=dist(cen,C)
    CD=dist(cen,B)
    ri=array([CA,CB,CC,CD])
    Rmax[ip]=amax(ri)
    Rmin[ip]=amin(ri)

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
    Rint[ip]=amin(h)


Aexp=4*pi/Npix
Rsq=sqrt(Aexp/2)
Rsqin=Rsq/sqrt(2)

#histo R
figure()

range=[0.6,1.5]
#hist(Rin/Rsqin,bins=80,range=range,label=r"$R_{in}$")
hist(Rint/Rsqin,bins=80,range=range,label="inner")
hist(Rmax/Rsq,color='red',bins=80,alpha=0.7,range=range,label="outer")

xlabel(r"radius")
legend()
xlim(range)
tight_layout()
#xticks(linspace(0.7,1.5,9))
semilogy()
show()
savefig("hp_radius.pdf")


print("Rmin={:.3f}".format(amin(Rint/Rsqin)))
print("Rmax={:.3f}".format(amax(Rmax/Rsq)))
