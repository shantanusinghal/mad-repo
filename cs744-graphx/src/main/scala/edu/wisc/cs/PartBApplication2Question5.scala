package edu.wisc.cs

import org.apache.spark.SparkContext
import org.apache.spark.graphx._

/**
  * Created by shantanusinghal on 29/11/17 @ 2:40 AM.
  * NET-ID: singhal5
  * Campus ID: 9076101956
  *
  * Question 5 - largest connected component
  */
object PartBApplication2Question5 {

  def main(args: Array[String]): Unit = {

    val sc = new SparkContext()

    val inputFile = sc.textFile(args(0))

    val (vertices, edges) = GraphUtils.parse(inputFile)

    val graph: Graph[List[String], String] = Graph(vertices, edges).cache()

    val output: Long = graph
      .connectedComponents()
      .vertices
      .map({
        case (vId, compId) => (compId, 1L)
      })
      .reduceByKey(_ + _)
      .reduce((a, b) => if (a._2 > b._2) a else b)
      ._2

    println(s"Size of the largest sub-graph which connects any two vertices is $output.")

    sc.stop()

  }
}
