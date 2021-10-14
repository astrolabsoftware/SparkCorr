#!/bin/bash
#SBATCH --qos=debug
#SBATCH --time=15
#SBATCH --nodes=1
#SBATCH --constraint=haswell


#module load python/3.8-anaconda-2020.11
#conda init
conda activate tc

export OMP_NUM_THREADS=32

time python treecorr_serial.py /global/cscratch1/sd/plaszczy/tomo100M.parquet
