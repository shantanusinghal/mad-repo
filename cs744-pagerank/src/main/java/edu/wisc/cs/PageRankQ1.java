package edu.wisc.cs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang.ArrayUtils;
import org.apache.spark.HashPartitioner;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import org.apache.spark.api.java.function.PairFunction;
import scala.Tuple2;

public class PageRankQ1 {

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

        JavaPairRDD<Integer, Integer[]> links = context.textFile(path, context.defaultParallelism())
            .filter(notComments) // RDD2 <String>
            .mapToPair(fromNode_ToNode) // RDD3 <Integer, Integer>
            .distinct(context.defaultParallelism()) // RDD4 <Integer, Integer>
            .partitionBy(new HashPartitioner(context.defaultParallelism())) // S-RDD5 <Integer, Integer>
            .mapValues(toList) // RDD6 <Integer, Integer[]>
            .reduceByKey(joinValues); // RDD7 <Integer, Integer[]>

        JavaPairRDD<Integer, Double> ranks = links
            .mapValues(returnSeedValue); // RDD8 <Integer, Double>

        for (int i = 0; i < iters; i++) {

          JavaPairRDD<Integer, Double> contributions = links
              .join(ranks) // RDD9 <Integer, (Integer[], Double)>
              .values() // RDD10 <Integer[], Double>
              .flatMapToPair(fromNode_contribValue); // RDD11 <Integer, Double>

          ranks = contributions
              .reduceByKey(sumContributions) // RDD12 <Integer, Double>
              .mapValues(calcRanks); // RDD13 <Integer, Double>
        }

        ranks
            .coalesce(1) // to enforce single output file
            .saveAsTextFile(conf.get("spark.local.dir") + "ranks");
      }
    }

  }

  /*
   * Private helper functions (lambdas) to provide sugar-syntax for PageRank logic
   */

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
