
from pylab import *
from tools import *
from scipy.interpolate import interp1d


def addStat(x,y):
        f=y/sum(y)
        xmean=sum(x*f)
        xx=x-xmean
        vx=sum(xx**2*f)
        sig=sqrt(vx)
        S=sum((xx/sig)**3*f)
        K=sum((xx/sig)**4*f)-3
        imax=y.argmax()
        xup=x[imax:]
        yup=y[imax:]
        x1=x[imax]
        if (len(xup)>5) :
            fup = interp1d(y[imax:],x[imax:])
            x1=fup(y[imax]/2)
        xd=x[0:imax+1]
        yd=y[0:imax+1]
        x2=x[imax]
        if (len(xd)>5) :
            fd = interp1d(y[0:imax+1],x[0:imax+1])
            x2=fd(y[imax]/2)

        fwhm=float(x1)-float(x2)

        stat=["N={:d}".format(int(sum(y))),"mode={:g}".format(x[imax]),"mean={:g}".format(xmean),"stdev={:g}".format(sqrt(vx)),"fwhm={:g}".format(fwhm),r"skew={:g}".format(S),r"kurt={:g}".format(K)]
        print(stat)
        ax=gca()
        text(0.7,0.7,"\n".join(stat), horizontalalignment='left',transform=ax.transAxes)



def hist_stat(x,y,label="",newFig=True,doStat=True,log=True):
    if newFig:
        figure()
    bar_outline(x,y,label=label)
    ylim(0.8*min(y),1.2*max(y))
    if log:
        yscale('log')
    xlabel(label)
        
    if doStat:
        addStat(x,y)

    show()
    return x,y



def hist_file(fn="df.txt",**kwargs):

    b,x,y=loadtxt(fn,unpack=True)
    return  hist_stat(x,y,**kwargs)
