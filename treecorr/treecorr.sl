#!/bin/bash
#SBATCH --qos=debug
#SBATCH --time=15
#SBATCH --nodes=1
#SBATCH --ntasks-per-node=32
#SBATCH --constraint=haswell

export OMP_NUM_THREADS=32

time python treecorr_serial.py
