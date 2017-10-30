# Structured Streaming

## Running
All the three scripts are using spark-submit to launch the streaming application. All the applications parameters are specified in the script to allows for quick changes/testing. Each scripts performs the following steps in order:

1. Clean/Create the HDFS files and directories they need, such as staging, monitoring, output, checkpoint, etc.
2. Populate the staging directory with the split Higgs dataset.
3. Start the streaming-emulator.
4. Start the streaming application.

The streaming emulator is configured to atomically move one split file from the staging to monitoring directory every 10 seconds. This value can be easily tweaked in each applications own script file.

```bash
.
├── Part-A
│   │
│   ├── PartAQuestion1.java
│   ├── PartAQuestion2.java
│   ├── PartAQuestion3.java
│   └── resources
│       ├── log4j.properties
│       ├── results.scala
│       ├── spark.properties
│       ├── split-dataset
│       │   ├── 1.csv
│       │   ├── 2.csv
│       │   ├── 3.csv
│       │   .
│       │   .
│       │
│       ├── twitter-stream-1.0-SNAPSHOT-jar-with-dependencies.jar
│       ├── twitter-stream-2.0-SNAPSHOT-jar-with-dependencies.jar
│       ├── twitter-stream-3.0-SNAPSHOT-jar-with-dependencies.jar
│       └── users.csv
│
├── PartAQuestion1.sh
├── PartAQuestion2.sh
├── PartAQuestion3.sh
└── streaming-emulator.sh
```

The `Part-A/resources` directory contains all the dependencies (runnable JARs and properties files) and utility scripts.

## Stopping
The application can be stopped using CTRL+C at which point the application will automatically stop the streaming-emulator. 

**Note**
In case the emulator does stop automatically you'll notice logs starting with [STREAM] continuing to appear on the console. You can use the following command to stop the emulator manually

```bash
ps -eaf | grep tweet-monitoring | awk '{print $2}' | xargs kill -9
```

## Question1
Location: ~/grader_assign2/Part-A/PartAQuestion1.java
I've create a model for Tweet to create a strongly typed dataset `Dataset<Tweet>`. I used groupBy() and window() operations to perform windowed aggregation with a 1 hours window, sliding every 30 minutes. The final result is sorted by the window time range to orderd the output chronologically. None of the cell values or rows are truncated and only minimal application logs are displayed. I've configured parallelism and other spark properties in `Part-A/resources/spark.properties` file to optimize cluster usage.


## Question2
Location: ~/grader_assign2/Part-A/PartAQuestion2.java
The strongly typed tweets dataset is created the same way as in Question 1. Because the distinct() command which isn't supported by Datasets/DataFrames I chained the following commands to emulate that behavior and calculate the list of users mentioned.

```bash
map (x -> <x, #>)
groupByKey ([x, <x, #>])
reduceGroup (<x, sum(#)>)
map (<x, sum(#)> -> x)
```

The stream is processed every 10 seconds using a ProcessingTime Trigger. The operations were chose carefully to support Append mode which is the only output mode that supports a file sink in `parquet` format. The parquet output can be viewed with the following command, which simply does a `select *` on the entire final table. Ensure that the hive metastore is running in the background before running this command.

```bash
spark-shell -i /home/ubuntu/grader_assign2/Part-A/resources/results.scala
```

## Question3
Location: ~/grader_assign2/Part-A/PartAQuestion3.java
This application works with two input streams, the same streaming dataset of tweets as before and another static dataset of users which is read from a csv file that contains one userId at each row. The list of users (located at `Part-A/resources/users.csv`) is randomly sampled from the dataset. 
The two datasets are joined using a left-semi join to select all the rows of the tweets dataset where the value for userA column matches with the user in users dataset. The joined tables are mapped to a new schema to better display the aggregate results in the following format

```bash
+------+--------+--------+--------+
|userId|sum(RTs)|sum(MTs)|sum(REs)|
+------+--------+--------+--------+
|1276  |0       |1       |5       |
```

The resulting dataset is grouped by the userId, summed across the various interaction dimensions and sorted by the userId to create the final output. The stream is processed every 5 seconds by using a ProcessingTime Trigger and the entire result is outputted to the console sink after each trigger.
