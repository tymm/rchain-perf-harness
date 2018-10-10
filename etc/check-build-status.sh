#!/bin/bash
set -axe

echo $pwd
total=`grep "request count" sbt.out | awk '{print $4}'`
errors=`grep "request count" sbt.out | sed -r 's/.*KO=([0-9]+).*/\1/'`
res=`echo $total $errors | awk '{ res = 0; start = NF / 2; for(i=1; i <= start; i++) {j=start+i; if(($i * 0.05) <= $j){res = 1}}; print res; }'`
exit $res
