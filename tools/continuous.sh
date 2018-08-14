#!/bin/bash
set -axe

java -Dhosts="localhost" -Dpath=$1 -Dsessions=1 -Ddeploy2ProposeRatio=10 -jar ./runner.jar 
