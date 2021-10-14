# Copyright (c) 2003-2019 by Mike Jarvis
#
# TreeCorr is free software: redistribution and use in source and binary forms,
# with or without modification, are permitted provided that the following
# conditions are met:
#
# 1. Redistributions of source code must retain the above copyright notice, this
#    list of conditions, and the disclaimer given in the accompanying LICENSE
#    file.
# 2. Redistributions in binary form must reproduce the above copyright notice,
#    this list of conditions, and the disclaimer given in the documentation
#    and/or other materials provided with the distribution.

# Run using:
#   mpiexec -n 4 python mpi_test.py
#
# Note for NERSC users: The conda (or pip) installed won't work correctly.
# You need to install mpi4py by hand using their cray compilers.  See:
# https://docs.nersc.gov/programming/high-level-environments/python/mpi4py/#mpi4py-in-your-custom-conda-environment
#
# See also the issue that I was having before resolving this:
# https://nersc.servicenowservices.com/nav_to.do?uri=%2Fincident.do%3Fsys_id%3Dcc9f38341be85c102548ea82f54bcbea%26sysparm_view%3Dess
#
# run with: 
# time mpiexec -n 4 python treecorr_mpi.py 

import time
import os
import sys
import shutil
import socket
import fitsio
import treecorr

from mpi4py import MPI
#   mpiexec -n 4 python mpi_test.py
#
# Note for NERSC users: The conda (or pip) installed won't work correctly.
# You need to install mpi4py by hand using their cray compilers.  See:
# https://docs.nersc.gov/programming/high-level-environments/python/mpi4py/#mpi4py-in-your-custom-conda-environment
#
# See also the issue that I was having before resolving this:
# https://nersc.servicenowservices.com/nav_to.do?uri=%2Fincident.do%3Fsys_id%3Dcc9f38341be85c102548ea82f54bcbea%26sysparm_view%3Dess

import time
import os
import sys
import shutil
import socket
import fitsio
import treecorr


# Some parameters you can play with here that will affect both serial (not really "serial", since
# it still uses OpenMP -- just running on 1 node) and parallel runs.
nbins=11
min_sep = 2.5         # arcmin
max_sep = 31.5
low_mem = False     # Set to True to use less memory during processing.

comm = MPI.COMM_WORLD
rank = comm.Get_rank()
nproc = comm.Get_size()

file_name= "~/data/tomo10M.parquet"
patch_file = os.path.basename(file_name).split(".")[0]+"_patches.fits"
ra_col='RA'
dec_col='DEC'
ra_units='degrees'
dec_units='degrees'

def make_patches():
    # First make the patches.  Do this on one process.
    # For a real-life example, this might be made once and saved.
    # Or it might be made from a smaller version of the catalog:
    # either with the every_nth option, or maybe on a redmagic catalog or similar,
    # which would be smaller than the full source catalog, etc.
    t0=time.time()

    if not os.path.exists(patch_file):
        print('Making patches')
        fname = file_name
        cat=treecorr.Catalog(file_name=file_name,ra_col=ra_col,dec_col=dec_col,
                             ra_units=ra_units,dec_units=dec_units,
                             verbose=2,npatch=32)


        print('Done loading file: nobj = ',cat.nobj,cat.ntot)
        cat.get_patches()
        print('Made patches: ',cat.patch_centers)
        cat.write_patch_centers(patch_file)
        print('Wrote patch file ',patch_file)
    else:
        print('Using existing patch file')

    t1=time.time()
    print("patches in {:g}s".format(t1-t0))


def run_serial():
    t0 = time.time()
    log_file = 'serial.log'

    #cat=treecorr.Catalog(file_name="tomo1M.parquet",ra_col="RA",dec_col="DEC",ra_units='degrees',dec_units='degrees',patch_centers="tomo1M_patches.fits")

    cat=treecorr.Catalog(file_name=file_name,ra_col=ra_col,dec_col=dec_col,
                         ra_units=ra_units,dec_units=dec_units,
                         patch_centers=patch_file,log_file=log_file,verbose=3)


    gg = treecorr.NNCorrelation(nbins=nbins, min_sep=min_sep,max_sep=max_sep,sep_units='arcmin', verbose=1, log_file=log_file)

    #gg=treecorr.NNCorrelation(min_sep=2.5,max_sep=250,nbins=20,sep_units='arcmin')


    # These next two steps don't need to be done separately.  They will automatically
    # happen when calling process.  But separating them out makes it easier to profile.
    # cat.load()
    # t2 = time.time()
    # print('Loaded', t2-t1)

    # cat.get_patches()
    # t3 = time.time()
    # print('Made patches', t3-t2)

    gg.process(cat)

    t1 = time.time()
    print("Done with non-parallel computation in {:g}s".format(t1-t0))

def run_parallel():

    t0 = time.time()
    print(rank,socket.gethostname(),flush=True)
    log_file = 'parallel_%d.log'%rank

    # All processes make the full cat with these patches.
    # Note: this doesn't actually read anything from disk yet.
    cat=treecorr.Catalog(file_name=file_name,ra_col=ra_col,dec_col=dec_col,
                         ra_units=ra_units,dec_units=dec_units,
                         patch_centers=patch_file,log_file=log_file,verbose=3)
    
    t1=time.time()

    cat.load()
    t2 = time.time()
    print(rank,'Load data', t2-t1, flush=True)

    cat.get_patches()
    t3 = time.time()
    print(rank,'Load patches', t3-t2, flush=True)

    gg = treecorr.NNCorrelation(nbins=nbins, min_sep=min_sep,max_sep=max_sep,sep_units='arcmin', verbose=1, log_file=log_file)

    # To use the multiple process, just pass comm to the process command.
    gg.process(cat, comm=comm)
    t4 = time.time()
    print(rank,'Processed', t4-t3, flush=True)
    comm.Barrier()
    t5 = time.time()
    print(rank,'Barrier', t5-t4, flush=True)
    print(rank,'Done with parallel computation',t5-t0,flush=True)

    # rank 0 has the completed result.
    if rank == 0:
        gg.write("out_mpi.csv")

if __name__ == '__main__':
    if rank == 0:
        make_patches()
        #run_serial()
    comm.Barrier()
    run_parallel()
