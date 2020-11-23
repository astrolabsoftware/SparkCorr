import pandas

pp=pandas.read_csv("p0_15.csv",sep="\s+")   
p1=pandas.read_csv("p5_10.csv",sep="\s+")                              
p2=pandas.read_csv("p10_15.csv",sep="\s+")                             
p12=pandas.concat([p1,p2])  

p=pp.merge(p12)

p=p.assign(rel=1-p.N1/p.N)  

pandas.options.display.float_format = '{:,.5f}'.format 

print(p)
