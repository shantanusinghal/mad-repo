#!/usr/bin/env bash

eval hadoop fs -rm -r /stream/monitoring/* > /dev/null
eval hadoop fs -rm -r /stream/staging/* > /dev/null
eval hadoop fs -copyFromLocal split-dataset/*.csv /stream/staging/ > /dev/null
eval ./stream.sh /stream/staging/ /stream/monitoring/ 10 > /dev/null 2>&1 &
spark-submit --master spark://10.254.0.187:7077 --properties-file spark-q1.properties --driver-java-options "-Dlog4j.configuration=file:///home/ubuntu/streaming/log4j-spark.properties" twitter-stream-1.0-SNAPSHOT-jar-with-dependencies.jar  hdfs://10.254.0.187:8020/stream/monitoring/

trap "ps -eaf | grep monitoring | awk '{print $2}' | xargs kill -9" SIGINT SIGTERM EXIT