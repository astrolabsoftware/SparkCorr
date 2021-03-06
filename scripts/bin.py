import pandas
pandas.options.display.float_format = '{:,.1f}'.format

p=pandas.read_csv("sa_binning.csv",sep="\s+")   


#cols=[pp for pp in p.columns if pp!="Nj" and pp !="NpixJ(k)"]  

cols=['id','td','tu','w','NpixD(M)','NpixJ(k)']




p[cols].to_latex("bin_setup.tex",index=False) 


p1=psa[['td','tu','w','Nd','NpixD(M)']] 
p2=pcs[['Nj', 'NpixJ(k)']]
pandas.concat([p1,p2],axis=1) 

p.to_latex("mixed_setup.tex",index=True) 
