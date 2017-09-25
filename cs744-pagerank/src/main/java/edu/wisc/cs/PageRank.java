package edu.wisc.cs;

import com.google.common.collect.Iterables;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
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

        JavaPairRDD<String, Iterable<String>> links = context.textFile(path)
            .filter(notComments)
            .mapToPair(fromNode_ToNodeList)
            .distinct()
            .groupByKey()
            .cache();

        JavaPairRDD<String, Double> ranks = links
            .mapValues(neighbours -> 1.0);

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
  private static Function<Double, Double> calcRanks = contrib -> 0.15 + contrib * 0.85;
  private static Function<String, Boolean> notComments = line -> !String.valueOf(line).startsWith("#");
  private static Function2<Double, Double, Double> sumContributions = (acc, val) -> (acc + val);
  private static PairFunction<String, String, String> fromNode_ToNodeList = line -> {
    String[] parts = line.split("\\s+");
    return new Tuple2<>(parts[0], parts[1]);
  };
  private static PairFlatMapFunction<Tuple2<Iterable<String>, Double>, String, Double> fromNode_contribValue = pair -> {
    int urlCount = Iterables.size(pair._1);
    return StreamSupport
        .stream(pair._1().spliterator(), false)
        .map(link -> new Tuple2<>(link, pair._2() / urlCount))
        .collect(Collectors.toList())
        .iterator();
  };


}
