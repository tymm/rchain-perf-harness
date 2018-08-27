#!/bin/bash
set -axe

buildNumber=$1
cmd=$2
contract=$3
image=$4

toRun=$cmd=/workdir/rchain-perf-harness/$contract

./drone-cli.sh deploy --param $toRun --param RNODE_IMAGE_VERSION=${image:-dev} lukasz-golebiewski-org/rchain-perf-harness $buildNumber custom_contract
