package edu.wisc.cs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang.ArrayUtils;
import org.apache.spark.Partitioner;
import org.apache.spark.RangePartitioner;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import org.apache.spark.api.java.function.PairFunction;
import scala.Tuple2;

/**
 * Computes the PageRank of URLs from an input file. Input file should
 * be in format of:
 * URL         neighbor URL
 * URL         neighbor URL
 * URL         neighbor URL
 * ...
 * where URL and their neighbors are separated by space(s).
 *
 */
public class PageRankQ2Y {

  public static void main(String[] args) {

    if (args.length < 2) {
      System.err.println("Usage: PartCQuestionX <path> <iters>");
      System.exit(1);
    }

    else {

      String path = args[0];
      SparkConf conf = new SparkConf();
      int iters = Integer.parseInt(args[1]) < 1 ? 10 : Integer.parseInt(args[1]);

      try (JavaSparkContext context = new JavaSparkContext(conf)) {

        /* RDD of (url, neighbour) pairs
           - number of partitions calculated as _____
           - spawn multiple concurrent tasks to try to load text-file directly into as many partitions.
           - used mapToPair instead of map because ___
           - used mapValues+reduceByKey instead of groupByKey to avoid indiscriminate shuffling
         */
        JavaPairRDD<Integer, Integer> pairs = context.textFile(path, context.defaultParallelism())
            .filter(notComments) // RDD2 <String>
            .mapToPair(fromNode_ToNode) // RDD3 <Integer, Integer>
            .distinct(context.defaultParallelism());

        JavaPairRDD<Integer, Integer[]> links = pairs
            .partitionBy(new RangePartitioner(
                context.defaultParallelism(),
                JavaPairRDD.toRDD(pairs),
                true,
                scala.math.Ordering.Int$.MODULE$,
                scala.reflect.ClassTag$.MODULE$.apply(Integer.class)))
            .mapValues(toList) // RDD4 <Integer, Integer[]>
            .reduceByKey(joinValues); // RDD5 <Integer, Integer[]>

        /* RDD of (url, rank) pairs
           - using partitioning to avoid repeatedly hashing
         */
        JavaPairRDD<Integer, Double> ranks = links
            .mapValues(returnSeedValue); // RDD6 <Integer, Double>

        for (int i = 0; i < iters; i++) {

//          context.parallelize(links
//              .join(ranks) // RDD7 <Integer, (Integer[], Double)>
//              .values() // RDD10 <Integer[], Double>
//              .flatMapToPair(fromNode_contribValue).partitions());

          JavaPairRDD<Integer, Double> contributions = links
              .join(ranks) // RDD7 <Integer, (Integer[], Double)>
              .values() // RDD10 <Integer[], Double>
              .flatMapToPair(fromNode_contribValue); // RDD11 <Integer, Double>

          ranks = contributions
              .reduceByKey(sumContributions) // RDD12 <Integer, Double>
              .mapValues(calcRanks); // RDD13 <Integer, Double>
        }

        ranks
            //.foreach(pair -> System.out.println(pair._1() + " rank: " + pair._2()));
            .coalesce(1)
            .saveAsTextFile(conf.get("spark.local.dir") + "ranks");
      }
    }

  }

  /*
   * Private helper functions for PageRank computations
   */

  private static class BucketPartitioner extends Partitioner {

    private final Integer numPartitions;

    public BucketPartitioner(Integer numPartitions) {
      this.numPartitions = numPartitions;
    }

    @Override
    public int numPartitions() {
      return this.numPartitions;
    }

    @Override
    public int getPartition(Object key) {
      return Integer.valueOf(((String) key)) % numPartitions();
    }
  }

  private static Function<Double, Double> calcRanks = new Function<Double, Double>() {
    @Override
    public Double call(Double contrib) throws Exception {
      return  0.15 + contrib * 0.85;
    }
  };

  private static Function<String, Boolean> notComments = new Function<String, Boolean>() {
    @Override
    public Boolean call(String line) throws Exception {
      return !String.valueOf(line).startsWith("#");
    }
  };

  private static Function2<Double, Double, Double> sumContributions = new Function2<Double, Double, Double>() {
    @Override
    public Double call(Double acc, Double val) {
      return acc + val;
    }
  };

  private static Function2<Integer[], Integer[], Integer[]> joinValues = new Function2<Integer[], Integer[], Integer[]>() {
    @Override
    public Integer[] call(Integer[] a1, Integer[] a2) throws Exception {
      return (Integer[]) ArrayUtils.addAll(a1, a2);
    }
  };

  private static Function<Integer[], Double> returnSeedValue = new Function<Integer[], Double>() {
    @Override
    public Double call(Integer[] neighbours) {
      return 1.0;
    }
  };

  private static PairFunction<String, Integer, Integer> fromNode_ToNode = new PairFunction<String, Integer, Integer>() {
    @Override
    public Tuple2<Integer, Integer> call(String line) {
      String[] parts = line.split("\\s+");
      return new Tuple2<>(Integer.valueOf(parts[0]), Integer.valueOf(parts[1]));
    }
  };

  private static Function<Integer, Integer[]> toList = new Function<Integer, Integer[]>() {
    @Override
    public Integer[] call(Integer val) {
      return new Integer[] {val};
    }
  };

  private static PairFlatMapFunction<Tuple2<Integer[], Double>, Integer, Double> fromNode_contribValue = new PairFlatMapFunction<Tuple2<Integer[], Double>, Integer, Double>() {
    @Override
    public Iterator<Tuple2<Integer, Double>> call(Tuple2<Integer[], Double> neighboursAndRankTuple) throws Exception {
      int urlCount = neighboursAndRankTuple._1().length;
      List<Tuple2<Integer, Double>> contributions = new ArrayList<>();
      for (Integer n : neighboursAndRankTuple._1) {
        contributions.add(new Tuple2<>(n, neighboursAndRankTuple._2 / urlCount));
      }
      return contributions.iterator();
    }
  };

}
