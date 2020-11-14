from pylab import *
import sys

import pandas

def getTimes(fn):
    p=pandas.read_csv(fn,sep="\s+")
    nodes=unique(p.nodes)
    tt=[]
    imin=p['imin'].values[1]
    imax=p['imax'].values[1]
    NpixD=float(p['NpixD'].values[1])/1e6
    NpixJ=float(p['NpixJ'].values[1])/1e3
    Ndata=float(p['Ndata'].values[1])/1e6
    for n in nodes:
        pn=p[p['nodes']==n] 
        tt.append([Ndata,n,imin,imax,NpixD,NpixJ,mean(pn.t),std(pn.t),mean(pn.ts),mean(pn.tr),mean(pn.td),mean(pn.tj),mean(pn.tb)])

    pt=pandas.DataFrame(data=tt,columns=["Ndata","nodes","imin","imax","NpixD","NpixJ","t","sig","tr","ts","td","tj","tb"])
    return pt


p=getTimes(sys.argv[1])

figure()
ind=[0,3,6]
t0=p.tr.values
t1=p.ts.values
t2=p.td.values
t3=p['tj'].values+p['tb'].values
sumt=t0+t1+t2+t3
t0/=sumt/100
t1/=sumt/100
t2/=sumt/100
t3/=sumt/100

bar(ind,t1,1,label="source")
bar(ind,t2,1,bottom=t1,label="duplicate")
bar(ind,t3,1,bottom=t1+t2,label="join")
bar(ind,t0,1,label="reduction",bottom=t1+t2+t3)
xticks(ind,p.nodes)
ylim(0,130)
legend()
title(sys.argv[1])
xlabel("#nodes")
ylabel("relative time (%)")
yticks(arange(6)*20)
show()

pandas.options.display.float_format = '{:,.1f}'.format

print(p[['Ndata','nodes','imin','imax','NpixD','NpixJ','t','sig']] )
