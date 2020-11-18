from pylab import *
from scipy import r_
import sys

class Point:
    def __init__(self, *args, **kwargs):
        if len(args) == 2:
            a=args[0]
            b=args[1]
            self.x=cos(b)*cos(a)
            self.y=cos(b)*sin(a)
            self.z=sin(b)
        else:
            self.x=args[0]
            self.y=args[1]
            self.z=args[2]      

    def __repr__(self):
            return f'Point({self.x},{self.y},{self.z})'
        

def subface0(signa,signb,N):
    n=N-1
    #print(f"n={n}")
    M=empty((N,N),dtype=Point)
    M[0,0]=Point(0,0)
    for i in range(1,N):
        gi=pi/12*(i/n)**2+pi/4
        alpha_i=signa*arccos(sqrt(2.)*cos(gi))
        #print(f"i={i}, gi={gi}, ai={alpha_i}")
        M[i,0]=Point(alpha_i,0)
        beta_ii=signb*arccos(1/(sqrt(2)*sin(gi)))
        M[i,i]=Point(alpha_i,beta_ii)
        for j in range(i+1):
            beta_ij=j*beta_ii/i
            M[i,j]=Point(alpha_i,beta_ij)
           #print(f"subface0 signs=({signa},{signb}) [{i},{j}]={M[i,j]}")
    #symetrize
    for i in range(1,N):
        for j in range(i):
            M[j,i]=Point(M[i,j].x,signa*signb*M[i,j].z,signa*signb*M[i,j].y)
    return M

###########
## M2 ] M0
## M3 ] M1
##########

#
def subfaceIndex(I,J):
    if J<=N :
        if I<=N:
            s=(3,N-I,N-J)
        else:
            s=(1,I-N,N-J)
    else :
        if I<=N:
            s=(2,N-I,J-N)
        else:
            s=(0,I-N,J-N)

    return s


rotator={1:lambda X,Y,Z:(-Y,X,Z), 2:lambda X,Y,Z:(-X,-Y,Z), 3:lambda X,Y,Z:(Y,-X,Z), 
             5:lambda X,Y,Z:(Z,Y,-X), 4:lambda X,Y,Z:(-Z,Y,X)}

def fillFace(face0,fnum):
    rot=rotator[fnum]

    face=empty_like(face0)
    Nx,Ny=face0.shape
    for i in range(Nx):
        for j in range(Ny):
            p=face0[i,j]
            x,y,z=rot(p.x,p.y,p.z)
            face[i,j]=Point(x,y,z)

    return face

########
#NPIX /face
assert len(sys.argv)==3
N=int(sys.argv[1])//2
fn=sys.argv[2]

# Nnodes= N+1

M0=subface0(1,1,N+1)
M1=subface0(1,-1,N+1)
M2=subface0(-1,1,N+1)
M3=subface0(-1,-1,N+1)

M=[M0,M1,M2,M3]

#
FACE0=empty((2*N+1,2*N+1),dtype=Point)
for I in range(2*N+1):
    for J in range(2*N+1):
        ff,i,j=subfaceIndex(I,J)
        FACE0[I,J]=M[ff][i,j]
        #print(f"FACE0[I={I} J={J}]={FACE0[I,J]}")

#

FACE1=fillFace(FACE0,1)
FACE2=fillFace(FACE0,2)
FACE3=fillFace(FACE0,3)
FACE4=fillFace(FACE0,4)
FACE5=fillFace(FACE0,5)

#FACES=[FACE0,FACE1,FACE2,FACE3,FACE4,FACE5]
#pour une seule face
FACES=[FACE0]

#write
print("writing "+fn)
f=open(fn,"w")
Nx,Ny=FACE0.shape

for FACE in FACES :
    for i in range(Nx):
        for j in range(Ny):
            line="{}\t{}\t{}".format(FACE[i,j].x,FACE[i,j].y,FACE[i,j].z)
            #print("node ({},{}) ".format(i,j)+line)
            f.write(line+"\n")
f.close()
