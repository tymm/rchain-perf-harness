#!/bin/bash
set -axe

buildNumber=$1
contract=$2
rchainCommitHash=$3

toRun=/workdir/rchain-perf-harness/$contract

./drone-cli.sh deploy --param CONTRACT=$toRun --param RCHAIN_COMMIT_HASH=${rchainCommitHash:-dev} lukasz-golebiewski/rchain-perf-harness $buildNumber custom_commit
