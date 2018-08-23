#!/bin/bash
if [ $# -ne 1 ]; then
  echo 1>&2 "Usage: $0 <docker-tag>"
  exit 3
fi

set -axe

#run sbt
#copy jar
#copy runner sh
#run docker

pushd ..
pushd templater
sbt -mem 4096 runner:assembly
popd
popd
if [ ! -d "runner" ]; then
    mkdir runner
fi
cp continuous.sh runner/
cp ../templater/runner/target/scala-2.12/runner.jar runner/

docker build -t rchain-perf-runner:$1 .