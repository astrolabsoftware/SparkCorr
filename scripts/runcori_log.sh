#!/bin/bash

declare -i nargs
nargs=$#

if [ $nargs -lt 2 ]; then
echo "##################################################################################"
echo "usage: "
echo "./${0##*/} imin imax (nside)"
echo "##################################################################################"
exit
fi

export SPARKVERSION=2.4.4
IMG=registry.services.nersc.gov/plaszczy/spark_desc:v$SPARKVERSION

prefix="log"
slfile="run_$prefix.sl"
echo $slfile
cat > $slfile <<EOF
#!/bin/bash

#SBATCH -q debug
#SBATCH -t 00:05:00
#SBATCH -N 16
#SBATCH -C haswell
#SBATCH -e slurm_${prefix}_%j.err
#SBATCH -o slurm_${prefix}_%j.out
#SBATCH --image=$IMG
#SBATCH --volume="/global/cscratch1/sd/$USER/tmpfiles:/tmp:perNodeCache=size=200G"

#init
source $HOME/desc-spark/scripts/init_spark.sh

#jars
LIBS=$HOME/SparkLibs
JARS=\$LIBS/jhealpix.jar,\$LIBS/spark-fits.jar,\$LIBS/spark3d.jar

#export FITSSOURCE="/global/cscratch1/sd/plaszczy/LSST10Y"
export INPUT="/global/cscratch1/sd/plaszczy/tomo100M.parquet"

#partitions
nodes=\$((\$SLURM_JOB_NUM_NODES-1))
ncores=\$((\$nodes*32))
part=\$ncores

shifter spark-shell $SPARKOPTS --jars \$JARS --conf spark.driver.args="${@:1} \$part" -I hpgrid.scala -I Timer.scala -i corr_loga.scala

stop-all.sh

EOF


cat $slfile

sbatch $slfile
