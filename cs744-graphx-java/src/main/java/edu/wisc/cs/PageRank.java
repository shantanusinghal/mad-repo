package edu.wisc.cs;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.graphx.Graph;
import org.apache.spark.graphx.GraphLoader;
import org.apache.spark.storage.StorageLevel;
import scala.reflect.ClassTag$;

/**
 * Created by shantanusinghal on 01/12/17 @ 7:03 PM.
 * NET-ID: singhal5
 * Campus ID: 9076101956
 */
public class PageRank {

  private static final double RESET_PROB = 0.15;

  public static void main(String[] args) {
    SparkConf conf = new SparkConf();

    try (JavaSparkContext context = new JavaSparkContext(conf)) {

      Graph<Object, Object> graph = GraphLoader.edgeListFile(
          context.sc(),
          args[0],
          false,
          GraphLoader.edgeListFile$default$4(),
          StorageLevel.MEMORY_AND_DISK_SER(),
          StorageLevel.MEMORY_AND_DISK_SER());

      Graph<Object, Object> run = org.apache.spark.graphx.lib.PageRank
          .run(graph,
              20,
              RESET_PROB,
              ClassTag$.MODULE$.Object(),
              ClassTag$.MODULE$.Object());

      run.vertices().saveAsTextFile(args[1]);

    }
  }

}
