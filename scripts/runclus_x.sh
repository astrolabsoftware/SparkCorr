# Copyright 2020 AstroLab Software
# Author: Stephane Plaszczynski
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


#!/bin/bash

declare -i nargs
nargs=$#

if [ $nargs -lt 3 ]; then
echo "##################################################################################"
echo "usage: "
echo "./${0##*/} tmin tmax Nbins "
echo "##################################################################################"
exit
fi

#optional

if [ -z "${ncores_tot}" ] ; then
echo "sparkopts!"
exit
fi
part=$((${ncores_tot}))


export INPUT="/lsst/tomo10M.parquet"
export SLURM_JOB_NUM_NODES=${n_executors}

spark-shell $SPARKOPTS --jars $JARS --conf spark.driver.args="$1 $2 $3" -I hpgrid.scala -I Timer.scala -i corr_x.scala
