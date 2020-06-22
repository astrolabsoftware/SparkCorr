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

if [ $nargs -eq 3 ] ;then
    myexec=corr_loga.scala
else
    myexec=corr_logx.scala
fi

#data
export INPUT="/global/cscratch1/sd/plaszczy/tomo100M.parquet"
data=$(basename $INPUT)
src=${data%%.parquet}

imin=$1
imax=$2

export SPARKVERSION=2.4.4
IMG=registry.services.nersc.gov/plaszczy/spark_desc:v$SPARKVERSION

prefix="${imin}-${imax}"
slfile="run_${src}_${prefix}.sl"
echo $slfile
cat > $slfile <<EOF
#!/bin/bash

#SBATCH -q debug
#SBATCH -t 00:05:00
#SBATCH -N 16
#SBATCH -C haswell
#SBATCH -e ${src}_${prefix}_%j.err
#SBATCH -o ${src}_${prefix}_%j.out
#SBATCH --image=$IMG
#SBATCH --volume="/global/cscratch1/sd/$USER/tmpfiles:/tmp:perNodeCache=size=200G"

#init
source $HOME/desc-spark/scripts/init_spark.sh

#jars
LIBS=$HOME/SparkLibs
JARS=\$LIBS/jhealpix.jar,\$LIBS/spark-fits.jar,\$LIBS/spark3d.jar


#partitions
nodes=\$((\$SLURM_JOB_NUM_NODES-1))
ncores=\$((3*\$nodes*32))
part=\$ncores

shifter spark-shell $SPARKOPTS --jars \$JARS --conf spark.driver.args="${@:1} \$part" -I hpgrid.scala -I Timer.scala -i $myexec

stop-all.sh

EOF


cat $slfile

sbatch $slfile
