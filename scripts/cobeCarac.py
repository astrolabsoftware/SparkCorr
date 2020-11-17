from pylab import *
from tools import *

from pylab import *

#
a=1/sqrt(3.)

#1 face
N=int(sys.argv[1])
#


#cobe
lx=linspace(-1.,1,N)
X,Y=meshgrid(lx,lx)

#mapping
#fortran indexing add 0
P=[0,-0.27292696,-0.07629969,-0.02819452,-0.22797056,-0.01471565,0.27058160,0.54852384,0.48051509,
       -0.56800938,-0.60441560,-0.62930065,-1.74114454,0.30803317,1.50880086,0.93412077,0.25795794,
       1.71547508,0.98938102,-0.93678576,-1.41601920,-0.63915306,0.02584375,-0.53022337,-0.83180469,
       0.08693841,0.33887446,0.52032238,0.14381585]

XX=X*X
YY=Y*Y

XI=X*(1.+(1.-XX)*(
   P[1]+XX*(P[2]+XX*(P[4]+XX*(P[7]+XX*(P[11]+XX*(P[16]+XX*P[22]))))) +
   YY*( P[3]+XX*(P[5]+XX*(P[8]+XX*(P[12]+XX*(P[17]+XX*P[23])))) +
   YY*( P[6]+XX*(P[9]+XX*(P[13]+XX*(P[18]+XX*P[24]))) +
   YY*( P[10]+XX*(P[14]+XX*(P[19]+XX*P[25])) + 
   YY*( P[15]+XX*(P[20]+XX*P[26]) +
   YY*( P[21]+XX*P[27] + YY*P[28])))) )))

ETA=Y*(1.+(1.-YY)*(
    P[1]+YY*(P[2]+YY*(P[4]+YY*(P[7]+YY*(P[11]+YY*(P[16]+YY*P[22]))))) +
    XX*( P[3]+YY*(P[5]+YY*(P[8]+YY*(P[12]+YY*(P[17]+YY*P[23])))) +
    XX*( P[6]+YY*(P[9]+YY*(P[13]+YY*(P[18]+YY*P[24]))) +
    XX*( P[10]+YY*(P[14]+YY*(P[19]+YY*P[25])) +
    XX*( P[15]+YY*(P[20]+YY*P[26]) +
    XX*( P[21]+YY*P[27] + XX*P[28])))) )))

x,y=a*XI,a*ETA



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



Aexp=4*pi/6/(N-1)**2
Rsq=sqrt(Aexp/2)
Rsqin=Rsq/sqrt(2)

imshowXY(arange(N-1),arange(N-1),Air/Aexp,vmin=0.85,vmax=1.15)
title("area")
yticks(gca().get_xticks())  
ylim(gca().get_xlim())
savefig("cobe_area2.pdf")

imshowXY(arange(N-1),arange(N-1),abs(e),vmin=0,vmax=0.3)
title("ellipticity")
yticks(gca().get_xticks())  
ylim(gca().get_xlim())
savefig("cobe_e2.pdf")

imshowXY(arange(N-1),arange(N-1),Rmax/Rsq,vmin=0.85,vmax=1.35)
title("outer radius")
yticks(gca().get_xticks())  
ylim(gca().get_xlim())
savefig("cobe_rout2.pdf")


imshowXY(arange(N-1),arange(N-1),Rint/Rsqin,vmin=0.7,vmax=1.1)
title("inner radius")
yticks(gca().get_xticks())  
ylim(gca().get_xlim())
savefig("cobe_rin2.pdf")

#histoR

figure()
axes([0.145,0.2,0.95-0.125,0.95-0.22])

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
savefig("cobe_radius.pdf")


print("Rmin={:.3f}".format(amin(Rint.flat/Rsqin)))
print("Rmax={:.3f}".format(amax(Rmax.flat/Rsq)))
