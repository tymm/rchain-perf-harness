#!/bin/bash
set -axe

while true; do 
    (curl http://bootstrap:39992 > bootstrap.txt) || true
    cat bootstrap.txt | curl --data-binary @- http://prometheus-pushgateway:9091/metrics/job/${DRONE_COMMIT_SHA}/instance/bootstrap
    (curl http://validator1:30012 > validator1.txt) || true
    cat validator1.txt | curl --data-binary @- http://prometheus-pushgateway:9091/metrics/job/${DRONE_COMMIT_SHA}/instance/validator1
    sleep 10
done