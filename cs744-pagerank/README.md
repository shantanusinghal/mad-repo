# PageRank

There are three implementations of PageRank using different strategies for managing partitioning schema and persistence. They all require the input text file to be in the following format:

page_ID         neighbor page_ID
page_ID         neighbor page_ID
page_ID         neighbor page_ID

...where page_IDs and their neighbors are separated by space(s). It is acceptable to have comments in the file that start with the character '#'

The output of each application is a single file that contains the ranks of each page_ID specified in the input. The output file is written to `${spark.local.dir}/ranks`

## Running

The application is packaged in runnable JARs that include all required dependencies. Three script file are provided to launch each application instance. These scripts use spark-submit to launch the PageRank application on a Spark Standalone cluster in client mode. The path of the input file and the number of iterations for convergence are specified as input parameters to the java application, heres how

```bash
# Running Question 1
./home/ubuntu/grader/PartCQuestion1.sh


# Running Question 2
./home/ubuntu/grader/PartCQuestion2.sh


# Running Question 3
./home/ubuntu/grader/PartCQuestion3.sh
```

## Question1
Location on master ~/grader/PartCQuestion-1.java
The first implementation of PageRank uses the default `HashPartitioner` to pre-partition the Links data structure and iterates the specified number of times to converge on a rank for all it's neighbours.

## Question2
Location on master ~/grader/PartCQuestion-2.java
The second implementation of PageRank uses a custom `BucketPartitioner` that tries to map pages from the same subdomain to the same partition. The remaining implementation remains the same as in the previous implementation. This implementation should deliver noticably better data locality due to it's improved partitioning schema.

## Question3
Location on master ~/grader/PartCQuestion-3.java
The third implementation of PageRank uses extends on the second one. It uses the `BucketPartitioner` for pre-partitioning the links data-structure and also caches it in RAM to allow for faster access. In-memory storage provides the most CPU efficiency and allows the RDDs to be processed much faster.


