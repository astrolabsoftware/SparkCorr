
import sys
from numpy import *

import pandas
data=sys.argv[1]
nodes=int(sys.argv[2])

p=pandas.read_csv(data,sep="\s+")

t=p[p['nodes']==nodes]['t'].values 

print("${:.1f}\pm{:.1f}$".format(t.mean(),t.std()))
