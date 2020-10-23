import pandas

#p=pandas.read_csv("binning.txt",sep="\s+")   
p=pandas.read_csv("bin_setup.csv")   

pandas.options.display.float_format = '{:,.1f}'.format


cols=[pp for pp in p.columns if pp!="Nc" and pp !="Nj"]  

cols=[pp for pp in p.columns if pp!="Nj" and pp !="NpixJ(k)"]  

p[cols].to_latex("bin_setup.tex",index=False) 
