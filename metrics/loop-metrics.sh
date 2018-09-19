#!/bin/bash
set -axe

while [ ! -f ./stop-loop ]; do
    for instance in bootstrap validator1 validator2 validator3 validator4; do
        (curl http://$instance:30012/metrics > $instance.txt) || true
        cat $instance.txt | curl --data-binary @- http://prometheus-pushgateway:9091/metrics/job/${DRONE_COMMIT_SHA}/instance/$instance
    done
    sleep 0.5
done
