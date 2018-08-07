#!/bin/bash

echo "setting up env in $1"
mkdir -p $1
echo "using templates from ./templates"
echo "generating bootstrap..."

echo "using certs from store ./store"
cp -R store $1/store

echo "setting up contracts..."
echo "using default contracts!"
cp -R templates/contracts $1/contracts

echo "setting up binaries..."
echo "assembly jar!"
cp bins/jars/rnode-assembly-0.5.3.jar $1/rnode.jar
