#!/bin/bash

#myexec="Tiling.CubedSphere"

myexec="Tiling.PixelSize"
#args="hp 128 10000000"
args="cs 180 10000000"


if [ -z "$SPARKOPTS" ] ; then
echo "missing SPARKOPTS"
exit
fi
# Package it
SBTVERSION=$(grep version build.sbt | awk '{print $3}')
VERSION=${SBTVERSION//\"/}
echo "Running version $VERSION"


SCALA_VERSION_SPARK=2.11
SCALA_VERSION=2.11.12

sbt ++${SCALA_VERSION} package

if [ $? -ne 0 ]; then exit; fi


# External JARS : in lib/
JARS=$(echo lib/*.jar)
MYJARS=${JARS// /,}

cmd="spark-submit $SPARKOPTS \
     --jars $MYJARS\
     --class com.sparkcorr.$myexec \
     $PWD/target/scala-${SCALA_VERSION_SPARK}/sparkcorr_${SCALA_VERSION_SPARK}-$VERSION.jar $args"


echo $cmd
eval time $cmd
