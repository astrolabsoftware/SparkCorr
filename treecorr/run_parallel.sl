#!/bin/bash
#SBATCH --qos=debug
#SBATCH --time=15
#SBATCH --nodes=16
#SBATCH --constraint=haswell


#module load python/3.8-anaconda-2020.11
#conda init
conda activate tc

nthreads=8
export OMP_NUM_THREADS=$nthreads

#attention nodes
time srun -n 16 -c $nthreads python treecorr_mpi.py /global/cscratch1/sd/plaszczy/tomo100M.parquet
