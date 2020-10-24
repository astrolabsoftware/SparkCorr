#!/bin/bash

#myexec="Tiling.CubedSphere"
#myexec="Tiling.PixelSize"

#myexec="Tiling.BenchPix"

myexec="2PCF.Sphere.PairCount_exact"
args=$*


echo "$myexec $args"


if [ -z "$SPARKOPTS" ] ; then
echo "missing SPARKOPTS"
exit
fi
# Package it
SBTVERSION=$(grep version build.sbt | awk '{print $3}')
VERSION=${SBTVERSION//\"/}
echo "Running SparkCorr version $VERSION"


SCALA_VERSION_SPARK=2.11
SCALA_VERSION=2.11.12

sbt ++${SCALA_VERSION} package

if [ $? -ne 0 ]; then exit; fi

cmd="spark-submit $SPARKOPTS \
     --jars lib/jhealpix.jar \
     --class com.sparkcorr.$myexec \
     $PWD/target/scala-${SCALA_VERSION_SPARK}/sparkcorr_${SCALA_VERSION_SPARK}-$VERSION.jar $args"


echo $cmd
eval time $cmd
