#!/bin/bash

declare -i nargs
nargs=$#

if [ $nargs -ne 1 ]; then
echo "##################################################################################"
echo "usage: "
echo "./${0##*/} parfile"
echo "##################################################################################"
exit
fi

myexec="2PCF.Sphere.PairCount_exact"
parfile=$1
echo "Running $myexec $parfile"


SBTVERSION=$(grep version build.sbt | awk '{print $3}')
VERSION=${SBTVERSION//\"/}
echo "SparkCorr version $VERSION"
SCALA_VERSION_SPARK=2.11

m=$(grep ^tiling $parfile | cut -d "=" -f2)
prefix="PairCountX_$m"

slfile="run_$prefix.sl"
echo $slfile

export SPARKVERSION=2.4.4
IMG=registry.services.nersc.gov/plaszczy/spark_desc:v$SPARKVERSION

cat > $slfile <<EOF
#!/bin/bash

#SBATCH -q debug
#SBATCH -t 00:10:00
#SBATCH -N 16
#SBATCH -C haswell
#SBATCH -e ${prefix}_%j.err
#SBATCH -o ${prefix}_%j.out
#SBATCH --image=$IMG
#SBATCH --volume="/global/cscratch1/sd/$USER/tmpfiles:/tmp:perNodeCache=size=200G"

#init
source $HOME/desc-spark/scripts/init_spark.sh

#jars
LIBS=$HOME/SparkLibs
JARS=\$LIBS/jhealpix.jar

#partitions
nodes=\$((\$SLURM_JOB_NUM_NODES-1))
ncores=\$((\$nodes*32))
part=\$((\$ncores*3))


shifter spark-submit $SPARKOPTS --jars \$JARS --class com.sparkcorr.$myexec $PWD/target/scala-${SCALA_VERSION_SPARK}/sparkcorr_${SCALA_VERSION_SPARK}-$VERSION.jar $parfile \$part



stop-all.sh

EOF


cat $slfile

sbatch $slfile
