package edu.wisc.cs;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.graphx.Graph;
import org.apache.spark.graphx.GraphLoader;
import org.apache.spark.graphx.lib.PageRank;
import org.apache.spark.storage.StorageLevel;
import scala.reflect.ClassTag$;

/**
 * Created by shantanusinghal on 28/11/17 @ 2:46 AM.
 * NET-ID: singhal5
 * Campus ID: 9076101956
 */
public class PageRank1 {

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

        Graph<Object, Object> run = PageRank
            .run(graph,
                20,
                RESET_PROB,
                ClassTag$.MODULE$.Object(),
                ClassTag$.MODULE$.Object());

//        Graph<Double, Double> seed = GraphUtils.prepare(graph);
//
//        Graph<Double, Double> ranks = GraphUtils.pageRank(seed, 20);
//
//        JavaRDD<Tuple2<Object, Double>> vertices = ranks.vertices().toJavaRDD();
//        // print to console
//        for (Tuple2<Object, Double> vertex : vertices.collect()) {
//          System.out.println(String.format("Vertex %s has rank %s", vertex._1(), vertex._2()));
//        }
        // save to disk
        run.vertices().saveAsTextFile(args[1]);

      }
  }

}
