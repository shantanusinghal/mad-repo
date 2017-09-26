#!/usr/bin/env bash
rm -rf out/*
mvn clean install
spark-submit --properties-file src/main/resources/q1-spark.properties target/pagerank-1.0.0-jar-with-dependencies.jar src/main/resources/web-BerkStan.txt 10
