import treecorr

fname="~/data/tomo10M.parquet"
nbins=11
sepmin=2.5
sepmax=31.5

gg=treecorr.NNCorrelation(min_sep=2.5,max_sep=250,nbins=20,sep_units='arcmin',verbose=2)

cat=treecorr.Catalog(file_name=fname,ra_col="RA",dec_col="DEC",ra_units='degrees',dec_units='degrees',verbose=2)

gg.process(cat)

gg.write("out_serial.csv")
