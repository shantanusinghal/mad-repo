package edu.wisc.cs;

import java.sql.Timestamp;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.catalyst.encoders.RowEncoder;
import org.apache.spark.sql.streaming.ProcessingTime;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.StreamingQueryException;
import org.apache.spark.sql.types.StructType;

/**
 * Created by shantanusinghal on 27/10/17 @ 1:26 PM.
 * NET-ID: singhal5
 * Campus ID: 9076101956
 */
public class PartAQuestion3 {

  public static final StructType HIGGS_DATASET_SCHEMA = new StructType()
      .add("userA", "string")
      .add("userB", "string")
      .add("timestamp", "date")
      .add("interaction", "string");

  public static final StructType USER_PROFILE_SCHEMA = new StructType()
      .add("userId", "string")
      .add("RTs", "integer")
      .add("MTs", "integer")
      .add("REs", "integer");

  public static void main(String[] args) throws StreamingQueryException {

    LogManager.getRootLogger().setLevel(Level.ERROR);

    String monitoringDir = args[0];
    String usersFilePath = args[1];

    SparkSession spark = SparkSession
        .builder()
        .appName("PartAQuestion1")
        .master("spark://10.254.0.187:7077")
        .getOrCreate();

    Dataset<String> users = spark
        .read()
        .schema(new StructType().add("user", "string"))
        .csv(usersFilePath)
        .as(Encoders.STRING())
        .cache();

    Dataset<Row> tweets = spark
        .readStream()
        .schema(HIGGS_DATASET_SCHEMA)
        .csv(monitoringDir);

    StreamingQuery query = tweets
        .join(users, tweets.col("userA").equalTo(users.col("user")), "leftsemi")
        .map(new MapFunction<Row, Row>() {
          @Override
          public Row call(Row r) throws Exception {
            String interaction = String.valueOf(r.getAs("interaction")).toUpperCase();
            return RowFactory.create(
                r.getAs("userA"),
                interaction.equals("MT") ? 1 : 0,
                interaction.equals("RT") ? 1 : 0,
                interaction.equals("RE") ? 1 : 0);
          }
        }, RowEncoder.apply(USER_PROFILE_SCHEMA))
        .groupBy("userId")
        .sum("RTs", "MTs", "REs")
        .sort("userId")
        .writeStream()
        .trigger(ProcessingTime.create("5 seconds"))
        .format("console")
        .outputMode("complete")
        .option("truncate", false)
        .option("numRows", Integer.MAX_VALUE - 1) // as many users as possible
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
