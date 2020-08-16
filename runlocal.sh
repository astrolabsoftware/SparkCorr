#!/bin/bash

myexec="ParamFile"
args=$*

# Package it
VERSION="0.1"
SCALA_VERSION_SPARK=2.11
SCALA_VERSION=2.11.12

sbt ++${SCALA_VERSION} package

if [ $? -ne 0 ]; then exit; fi

if [ -z "$SPARKOPTS" ] ; then
SPARKOPTS="--master local[*] --driver-class-path=$PWD "
fi

cmd="spark-submit $SPARKOPTS \
     --class com.sparkcorr.IO.$myexec \
     $PWD/target/scala-${SCALA_VERSION_SPARK}/sparkcorr_${SCALA_VERSION_SPARK}-$VERSION.jar $args"


echo $cmd
eval $cmd
