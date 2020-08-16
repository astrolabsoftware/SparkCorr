#!/bin/bash

VERSION="0.1"

myexec="ParamFile"

# Package it
SCALA_VERSION_SPARK=2.11
SCALA_VERSION=2.11.12

sbt ++${SCALA_VERSION} package

if [ $? -ne 0 ]; then exit; fi

if [ -z "$SPARKOPTS" ] ; then
SPARKOPTS="--master local[*] --driver-class-path=$PWD "
fi

cmd="spark-submit $SPARKOPTS \
     --class com.sparkcorr.IO.$myexec \
     $PWD/target/scala-${SCALA_VERSION_SPARK}/sparkcorr_${SCALA_VERSION_SPARK}-$VERSION.jar $PWD/test.txt"


echo $cmd
eval $cmd
