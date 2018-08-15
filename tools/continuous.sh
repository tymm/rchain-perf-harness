#!/bin/bash
set -axe

java -Dhosts="localhost" -Dpath=$1 -Dsessions=5 -Dratio=10 -jar ./runner.jar 
