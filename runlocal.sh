#!/bin/bash

myexec="Tiling.CubedSphere"

#myexec="Tiling.PixelSize"
#args="hp 128 100000000"
#args="cs 180 100000000"
#args="sars 180 100000000"

#myexec="Tiling.SARSPix"

#myexec="Tiling.BenchPix"

#myexec="Binning.LogBinning"

myexec="2PCF.BinSetup"
#myexec="2PCF.Sphere.PairCount_exact"
args=$*


echo "$myexec $args"

# Package it: veriosn in phase with sbt
SBTVERSION=$(grep version build.sbt | awk '{print $3}')
VERSION=${SBTVERSION//\"/}
echo "Running version $VERSION"

SCALA_VERSION_SPARK=2.11
SCALA_VERSION=2.11.12

sbt ++${SCALA_VERSION} package

if [ $? -ne 0 ]; then exit; fi

if [ -z "$SPARKOPTS" ] ; then
SPARKOPTS="--master local[*] --driver-class-path=$PWD "
fi

cmd="spark-submit $SPARKOPTS \
     --jars lib/jhealpix.jar \
     --class com.sparkcorr.$myexec \
     $PWD/target/scala-${SCALA_VERSION_SPARK}/sparkcorr_${SCALA_VERSION_SPARK}-"$VERSION.jar" $args"


echo $cmd
eval $cmd
