from pylab import *

rad2arcmin=lambda x: rad2deg(x)*60 
arcmin2rad=lambda x: deg2rad(x/60) 


def Rmax2N(arcmin):
    R=arcmin2rad(arcmin)
    N=floor((1.26*sqrt(pi/3)/R)).astype('int')
    return N

def Rmax2Npix_cs(arcmin):
     R=arcmin2rad(arcmin)
     return 2*pi/(R/0.85)**2

def Rmax2Npix_hp(arcmin):
     R=arcmin2rad(arcmin)
     return 2*pi/(R/0.9)**2

def N2Rmax(N):
    return rad2arcmin(1.26*sqrt(pi/3)/N)

def Rmax2Nside(arcmin):
    R=arcmin2rad(arcmin)
    N=1.45*sqrt(pi/6)/R
    nside=(2**floor(log2(N))).astype('int')
    return N.astype('int')
    
def Rmin2Nside(arcmin):
    R=arcmin2rad(arcmin)
    N=0.9*sqrt(pi/6)/R
    nside=(2**floor(log2(N))).astype('int')
    return nside

def Rmin2N(arcmin):
    R=arcmin2rad(arcmin)
    N=(0.85*sqrt(pi/6)/R).astype('int')
    return N
    
def Nside2Rmax(nside):
    return rad2arcmin(1.45*sqrt(2*pi/(12*nside**2)))


#
t=logspace(log10(2.5),log10(250),21) 


#cs
N=Rmin2N(t/2)
npix_cs=6*N**2

nside=Rmin2Nside(t/2)
npix_hp=12*nside**2

loglog(t,npix_cs,'k',label="CubedSphere")
loglog(t,npix_cs,'ko')

#hp
loglog(t,npix_hp,'--',color='grey',label="Healpix")
loglog(t,npix_hp,'o--',color='grey')

xlabel(r"$\theta_{max} \quad [arcmin]$")
ylabel("Npix")

legend()
tight_layout()
show()
