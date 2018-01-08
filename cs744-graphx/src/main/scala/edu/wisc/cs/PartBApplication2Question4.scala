package edu.wisc.cs

import org.apache.spark.SparkContext
import org.apache.spark.graphx._

/**
  * Created by shantanusinghal on 29/11/17 @ 2:40 AM.
  * NET-ID: singhal5
  * Campus ID: 9076101956
  *
  * Question 4 - most common word
  */
object PartBApplication2Question4 {

  def main(args: Array[String]): Unit = {

    val sc = new SparkContext("local[2]", "del")

    val inputFile = sc.textFile(args(0))

    val (vertices, edges) = GraphUtils.parse(inputFile)

    val graph: Graph[List[String], String] = Graph(vertices, edges).cache()

    val output: String = graph
      .vertices
      .flatMap(_._2)
      .countByValue()
      .reduce((a, b) => if (a._2 > b._2) a else b)
      ._1

    println(s"The most popular word across all the tweets is '$output'.")

    sc.stop()
  }
}
