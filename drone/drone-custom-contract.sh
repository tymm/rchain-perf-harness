#!/bin/bash
set -axe

buildNumber=$1
contract=$2
image=$3

./drone-cli.sh deploy --param RNODE_IMAGE_VERSION=${image:-dev} --param CONTRACT=/workdir/rchain-perf-harness/$contract lukasz-golebiewski-org/rchain-perf-harness $buildNumber custom-contract
