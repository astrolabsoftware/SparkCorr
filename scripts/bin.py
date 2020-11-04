import pandas
pandas.options.display.float_format = '{:,.1f}'.format

p=pandas.read_csv("sa_binning.csv",sep="\s+")   


#cols=[pp for pp in p.columns if pp!="Nj" and pp !="NpixJ(k)"]  

cols=['w','Nd', 'NpixD(M)','tu','Nj', 'NpixJ(k)']





p[cols].to_latex("hp_setup.tex",index=True) 
