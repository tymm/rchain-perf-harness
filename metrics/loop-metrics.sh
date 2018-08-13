#!/bin/bash
set -axe

while true; do 
    curl http://bootstrap:39992 || true > bootstrap.txt
    cat bootstrap.txt | curl --data-binary @- http://prometheus-pushgateway:9091/metrics/job/${DRONE_COMMIT_SHA}/instance/bootstrap || true
    curl http://validator1:30012 || true > validator1.txt
    cat validator1.txt | curl --data-binary @- http://prometheus-pushgateway:9091/metrics/job/${DRONE_COMMIT_SHA}/instance/validator1 || true
    sleep 10
done