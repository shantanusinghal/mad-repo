#!/bin/bash
echo "[START-STREAM]"

srcDir=$1
destDir=$2
interval=$3
numFiles="$(hadoop fs -count /stream/staging)"

for ((i=1; i<=numFiles; i++)); do
    file=${i}.txt
    echo "$(date +"%T") copying ${file}"
    hadoop fs -cp ${srcDir}${file} ${destDir}
    sleep ${interval}
done

echo "[END-STREAM]"
