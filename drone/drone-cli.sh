#!/bin/bash

if [ -z ${DRONE_TOKEN+x} ]; then echo "Export the DRONE_TOKEN"; exit 1; fi

export DRONE_SERVER=http://stress-docker.pyr8.io:8080
docker run --rm -e DRONE_SERVER=$DRONE_SERVER -e DRONE_TOKEN=$DRONE_TOKEN drone/cli ${@:1:99}
