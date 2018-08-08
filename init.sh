#!/bin/bash
set -axe

if [ -z ${RNODE_JAR+x} ]; then echo "Export the RNODE_JAR"; exit 1; else echo "RNODE_JAR is set to '$RNODE_JAR'"; fi
if [ -z ${1+x} ]; then echo "Provide the test environment directory to use"; exit 1; else echo "Test environment dir is set to '$1'"; fi

envName=$1

pushd templater
sbt "; project templater; run --out ../builder/envs/$envName"
popd
pushd builder
cp $RNODE_JAR ./bins/jars/
sh setup-env.sh envs/$envName
pushd envs/$envName
./run-env.sh
echo "waiting for network to get up."
sleep 20
./test.sh
