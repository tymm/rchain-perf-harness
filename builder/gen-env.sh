#!/bin/bash

echo "setting up env in $1"
mkdir -p envs/$1
echo "using templates from ./templates"
echo "generating bootstrap..."

echo "generating $2 nodes..."

echo "using certs from store ./store"
cp -R store envs/$1/store

echo "setting up contracts..."
echo "using default contracts!"
cp -R templates/contracts envs/$1/contracts

echo "setting up binaries..."
echo "assembly jar!"
cp bins/rnode-jar envs/$1/rnode
cp bins/jars/rnode-assembly-0.5.3.jar envs/$1/rnode.jar
cp -R templates/actions envs/$1/actions
