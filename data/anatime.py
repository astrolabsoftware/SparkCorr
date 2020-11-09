
from pylab import *
import sys

import pandas

def getTimes(fn):
    p=pandas.read_csv(fn,sep="\s+")
    nodes=unique(p.nodes)
    tt=[]
    for n in nodes:
        pn=p[p['nodes']==n] 
        tt.append([n,mean(pn.t),std(pn.t),mean(pn.ts),mean(pn.td),mean(pn.tj),mean(pn.tb)])

    pt=pandas.DataFrame(data=tt,columns=["nodes","t","sig","ts","td","tj","tb"])
    return pt


p=getTimes(sys.argv[1])
#p1=getTimes("tomo500M_0_5.dat")  
#p2=getTimes("tomo500M_0_5.dat")  
#p3=getTimes("tomo1G_0_1.dat")

figure()
ind=[0,3,6]
t1=p.ts.values
t2=p.td.values
t3=p['tj'].values+p['tb'].values
sumt=t1+t2+t3
t1/=sumt/100
t2/=sumt/100
t3/=sumt/100

bar(ind,t1,1,label="source")
bar(ind,t2,1,bottom=t1,label="duplicate")
bar(ind,t3,1,bottom=t1+t2,label="join")
xticks(ind,p.nodes)
ylim(0,130)
legend()
title(sys.argv[1])
xlabel("#nodes")
ylabel("relative time (%)")
yticks(arange(6)*20)
show()

pandas.options.display.float_format = '{:,.1f}'.format

print(p[['nodes','t','sig']] )
