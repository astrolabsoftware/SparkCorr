from pylab import *
from tools import *
import pandas as pd


fig_width_pt=256.0748
inches_per_pt = 1.0/72.27               # Convert pt to inches
golden_mean = (sqrt(5)-1.0)/2.0         # Aesthetic ratio
ratio=0.7
fig_width = fig_width_pt*inches_per_pt  # width in inches
fig_height =fig_width*ratio       # height in inches
fig_size = [fig_width,fig_height]
params = {'backend': 'ps',
          'axes.labelsize': 10,
          'legend.fontsize': 10,
          'font.size':10,
          'xtick.labelsize': 8,
          'ytick.labelsize': 8,
#          'text.usetex': True,
          'figure.figsize': fig_size}
rcParams.update(params)
figure()
offset=0.15
#axes([0.2,0.2,0.95-0.2,0.95-0.2])

#axes([0.145,0.2,0.95-0.125,0.95-0.22])
#ticklabel_format(axis='y',style='sci',scilimits=(0,0))
