#!/bin/bash
set -axe

java -Dhosts="$1" -Dpath=$2 -Dsessions=$3 -Dratio=$4 -Dloops=$5 -jar ./runner.jar 
