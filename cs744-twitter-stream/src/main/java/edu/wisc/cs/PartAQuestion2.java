package edu.wisc.cs;

import java.sql.Timestamp;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.spark.api.java.function.FilterFunction;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.api.java.function.ReduceFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.catalyst.encoders.ExpressionEncoder;
import org.apache.spark.sql.streaming.ProcessingTime;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.StreamingQueryException;
import org.apache.spark.sql.types.StructType;
import scala.Tuple2;

/**
 * Created by shantanusinghal on 27/10/17 @ 1:26 PM.
 * NET-ID: singhal5
 * Campus ID: 9076101956
 */
public class PartAQuestion2 {

    public static final StructType HIGGS_DATASET_SCHEMA = new StructType()
        .add("userA", "string")
        .add("userB", "string")
        .add("timestamp", "date")
        .add("interaction", "string");

  public static void main(final String[] args) throws StreamingQueryException {

    LogManager.getRootLogger().setLevel(Level.ERROR);

    String monitoringDir = args[0];
    String outputDir = args[1] + "/output/";
    String checkpointDir = args[1] + "/checkpoint/";

    SparkSession spark = SparkSession
        .builder()
        .appName("PartAQuestion2")
        .master("spark://10.254.0.187:7077")
        .getOrCreate();

    Dataset<Tweet> tweets = spark
        .readStream()
        .schema(HIGGS_DATASET_SCHEMA)
        .csv(monitoringDir)
        .as(ExpressionEncoder.javaBean(Tweet.class));

    StreamingQuery query = tweets
        .filter(new FilterFunction<Tweet>() {
          @Override
          public boolean call(Tweet value) throws Exception {
            return value.getInteraction().equals("MT");
          }
        })
        // using map-groupByKey-reduceGroups-map to simulate distinct
        // using transformations that maintain compatibility with 'append' mode
        .map(new MapFunction<Tweet, Tuple2<String, Integer>>() {
          @Override
          public Tuple2<String, Integer> call(Tweet tweet) throws Exception {
            return new Tuple2<>(tweet.getUserB(), 1);
          }
        }, Encoders.tuple(Encoders.STRING(), Encoders.INT()))
        .groupByKey(new MapFunction<Tuple2<String, Integer>, String>() {
          @Override
          public String call(Tuple2<String, Integer> value) throws Exception {
            return value._1();
          }
        }, Encoders.STRING())
        .reduceGroups(new ReduceFunction<Tuple2<String, Integer>>() {
          @Override
          public Tuple2<String, Integer> call(Tuple2<String, Integer> v1,
              Tuple2<String, Integer> v2)
              throws Exception {
            return new Tuple2<>(v1._1(), v1._2() + v2._2());
          }
        })
        .map(new MapFunction<Tuple2<String, Tuple2<String, Integer>>, String>() {
          @Override
          public String call(Tuple2<String, Tuple2<String, Integer>> value) throws Exception {
            return value._1();
          }
        }, Encoders.STRING())
        .writeStream()
        .trigger(ProcessingTime.create("10 seconds"))
        .format("parquet")
        .option("checkpointLocation", checkpointDir)
        .option("path", outputDir)
        .option("truncate", false)
        .start();

    query.awaitTermination();

  }

  /**
   * Created by shantanusinghal on 26/10/17 @ 8:44 PM.
   * NET-ID: singhal5
   * Campus ID: 9076101956
   */
  public static class Tweet {

    private String userA;
    private String userB;
    private Timestamp timestamp;
    private String interaction;

    public void setUserA(String userA) {
      this.userA = userA;
    }

    public void setUserB(String userB) {
      this.userB = userB;
    }

    public void setTimestamp(Timestamp timestamp) {
      this.timestamp = timestamp;
    }

    public void setInteraction(String interaction) {
      this.interaction = interaction;
    }

    public String getUserA() {
      return userA;
    }

    public String getUserB() {
      return userB;
    }

    public Timestamp getTimestamp() {
      return timestamp;
    }

    public String getInteraction() {
      return interaction;
    }
  }

}
