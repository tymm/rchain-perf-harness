#!/bin/bash
set -axe

#run sbt
#copy jar
#copy runner sh
#run docker

pushd ..
pushd templater
sbt runner:assembly
popd
popd
mkdir runner
cp continuous.sh runner/
cp ../templater/runner/target/scala-2.12/runner.jar runner/

docker build -t rchain-perf-runner:$1 .