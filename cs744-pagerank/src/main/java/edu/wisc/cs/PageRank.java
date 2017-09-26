package edu.wisc.cs;

import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
public class PageRank {

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

        // RDD of (url, neighbour) pairs
        JavaPairRDD<String, Iterable<String>> links = context.textFile(path, 20)
            .filter(notComments)
            .mapToPair(fromNode_ToNodeList)
            .distinct()
            .groupByKey()
            .cache(); // Using cache() to keep neighbor lists in RAM

        // RDD of (url, rank) pairs
        JavaPairRDD<String, Double> ranks = links.mapValues(returnSeedValue);

        for (int i = 0; i < iters; i++) {

          JavaPairRDD<String, Double> contributions = links
              .join(ranks)
              .values()
              .flatMapToPair(fromNode_contribValue);

          ranks = contributions
              .reduceByKey(sumContributions)
              .mapValues(calcRanks);
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

  private static Function<Iterable<String>, Double> returnSeedValue = new Function<Iterable<String>, Double>() {
    @Override
    public Double call(Iterable<String> neighbours) {
      return 1.0;
    }
  };

  private static PairFunction<String, String, String> fromNode_ToNodeList = new PairFunction<String, String, String>() {
    @Override
    public Tuple2<String, String> call(String line) {
      String[] parts = line.split("\\s+");
      return new Tuple2<>(parts[0], parts[1]);
    }
  };

  private static PairFlatMapFunction<Tuple2<Iterable<String>, Double>, String, Double> fromNode_contribValue = new PairFlatMapFunction<Tuple2<Iterable<String>, Double>, String, Double>() {
    @Override
    public Iterator<Tuple2<String, Double>> call(Tuple2<Iterable<String>, Double> neighboursAndRankTuple) throws Exception {
      int urlCount = Iterables.size(neighboursAndRankTuple._1);
      List<Tuple2<String, Double>> contributions = new ArrayList<>();
      for (String n : neighboursAndRankTuple._1) {
        contributions.add(new Tuple2<>(n, neighboursAndRankTuple._2 / urlCount));
      }
      return contributions.iterator();
    }
  };

}
