from pylab import *
from tools import *

assert len(sys.argv)==2
fn=sys.argv[1]

#read file
print("reading "+fn)

#nodes : there Npix+1 of thgem
rawnodes=loadtxt(fn)

N=int(sqrt(rawnodes.shape[0]))
nodes=rawnodes.reshape(N,N,3)


Npix=N-1

def dist(p1,p2):
    return sqrt(sum((p1-p2)**2))

def dist2(p1,p2):
    return sum((p1-p2)**2)

#base =c
def hauteur(a,b,c) :
    p=(a+b+c)/2
    return 2*sqrt(p*(p-a)*(p-b)*(p-c))/c


#cells
Air=zeros((N-1,N-1))
e=zeros((N-1,N-1))
Rmax=zeros((N-1,N-1))
Rint=zeros((N-1,N-1))
#for ip in range(N*N):
#    i,j=ip//N,ip%N
    
for i in range(N-1):
    for j in range(N-1):

        A=nodes[i,j]
        B=nodes[i+1,j]
        C=nodes[i+1,j+1]
        D=nodes[i,j+1]

        p2=dist2(A,C)
        q2=dist2(B,D)

        a2=dist2(A,B)
        b2=dist2(B,C)
        c2=dist2(C,D)
        d2=dist2(D,A)

        Air[i,j]=sqrt(4*p2*q2-(b2+d2-a2-c2)**2)/4

        p=sqrt(p2)
        q=sqrt(q2)
        e[i,j]=(p-q)/(p+q)

        #center
        cen=(A+B+C+D)/4.
        cen=cen/sqrt(sum(cen**2))
        
        OA=dist(cen,A)
        OB=dist(cen,B)
        OC=dist(cen,C)
        OD=dist(cen,D)
        ri=array([OA,OB,OC,OD])
        #si=2*arcsin(ri/2)
        
        Rmax[i,j]=amax(ri)

        h1=hauteur(OA,OB,sqrt(a2))
        h2=hauteur(OB,OC,sqrt(b2))
        h3=hauteur(OC,OD,sqrt(c2))
        h4=hauteur(OD,OA,sqrt(d2))
   
        h=array([h1,h2,h3,h4])
        Rint[i,j]=amin(h)




Aexp=4*pi/6/(N-1)**2
Rsq=sqrt(Aexp/2)
Rsqin=Rsq/sqrt(2)

imshowXY(arange(N-1),arange(N-1),Air/Aexp,vmin=0.85,vmax=1.15)
title("area")
yticks(gca().get_xticks())  
ylim(gca().get_xlim())
savefig("sars_area2.pdf")

imshowXY(arange(N-1),arange(N-1),abs(e),vmin=0,vmax=0.3)
title("ellipticity")
yticks(gca().get_xticks())  
ylim(gca().get_xlim())
savefig("sars_e2.pdf")

imshowXY(arange(N-1),arange(N-1),Rmax/Rsq,vmin=0.85,vmax=1.35)
title("outer radius")
yticks(gca().get_xticks())  
ylim(gca().get_xlim())
savefig("sars_rout2.pdf")


imshowXY(arange(N-1),arange(N-1),Rint/Rsqin,vmin=0.7,vmax=1.1)
title("inner radius")
savefig("sars_rin2.pdf")
yticks(gca().get_xticks())  
ylim(gca().get_xlim())
#histo R
figure()

#axes([0.10,0.2,0.8,0.73])
axes([0.10,0.2,0.85,0.75])
range=[0.6,1.5]
hist(Rint.flat/Rsqin,bins=80,range=range,label="inner")
hist(Rmax.flat/Rsq,color='red',bins=80,alpha=0.7,range=range,label="outer")

xlabel(r"radius")
legend()
xlim(range)
#xticks(linspace(0.7,1.5,9))
semilogy()
show()
savefig("sars_radius.pdf")


print("Rin={:.3f}".format(amin(Rint.flat/Rsqin)))
print("Rout={:.3f}".format(amax(Rmax.flat/Rsq)))
