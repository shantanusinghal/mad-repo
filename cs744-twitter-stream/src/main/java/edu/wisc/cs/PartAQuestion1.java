package edu.wisc.cs;

import java.sql.Timestamp;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.catalyst.encoders.ExpressionEncoder;
import org.apache.spark.sql.functions;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.StreamingQueryException;
import org.apache.spark.sql.types.StructType;

/**
 * Created by shantanusinghal on 27/10/17 @ 1:26 PM.
 * NET-ID: singhal5
 * Campus ID: 9076101956
 */
public class PartAQuestion1 {

    public static final StructType HIGGS_DATASET_SCHEMA = new StructType()
        .add("userA", "string")
        .add("userB", "string")
        .add("timestamp", "date")
        .add("interaction", "string");

  public static void main(String[] args) throws StreamingQueryException {

    LogManager.getRootLogger().setLevel(Level.ERROR);

    String monitoringDir = args[0];

    SparkSession spark = SparkSession
        .builder()
        .appName("PartAQuestion1")
        .master("spark://10.254.0.187:7077")
        .getOrCreate();

    Dataset<Tweet> tweets = spark
        .readStream()
        .schema(HIGGS_DATASET_SCHEMA)
        .csv(monitoringDir)
        .as(ExpressionEncoder.javaBean(Tweet.class));

    StreamingQuery query = tweets
        .groupBy(
            functions.window(tweets.col("timestamp"), "1 hours", "30 minutes"),
            tweets.col("interaction"))
        .count()
        .sort("window")
        .writeStream()
        .format("console")
        .outputMode("complete")
        .option("truncate", false)
        .option("numRows", 1008) // (24*2) windows * 3 categories * 7 days
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
