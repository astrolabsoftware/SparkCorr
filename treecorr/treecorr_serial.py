import treecorr
import sys,os

fname=sys.argv[1]
nbins=20
sepmin=2.5
sepmax=250
bin_type="Log"
#sepmax=31.5
#bin_type="Linear"

gg=treecorr.NNCorrelation(min_sep=sepmin,max_sep=sepmax,nbins=nbins,bin_type=bin_type,sep_units='arcmin',verbose=2)

cat=treecorr.Catalog(file_name=fname,ra_col="RA",dec_col="DEC",ra_units='degrees',dec_units='degrees',verbose=2)

gg.process(cat)
outfile= "out_"+os.path.basename(fname).split(".")[0]+".csv"

gg.write(outfile)
