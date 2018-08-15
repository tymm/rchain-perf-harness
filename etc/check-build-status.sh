#!/bin/bash
set -axe

echo $pwd
total=`grep "request count" sbt.out | awk '{print $4}'`
errors=`grep "request count" sbt.out | sed -r 's/.*KO=([0-9]+).*/\1/'`
res=`echo $total $errors | awk '{if (($1 * 0.05) > $2) {print 0} else {print 1}}'`
exit $res
