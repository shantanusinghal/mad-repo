package edu.wisc.cs

import org.apache.spark.SparkContext
import org.apache.spark.graphx._
import org.apache.spark.rdd.RDD

/**
  * Created by shantanusinghal on 29/11/17 @ 2:40 AM.
  * NET-ID: singhal5
  * Campus ID: 9076101956
  *
  * Question 2 - most popular vertex
  */
object PartBApplication2Question2 {
  val TAG = "a2-q2/"

  def main(args: Array[String]): Unit = {

    val sc = new SparkContext()

    val inputFile = sc.textFile(args(0))

    val (vertices, edges) = GraphUtils.parse(inputFile)

    val graph: Graph[List[String], String] = Graph(vertices, edges).cache()

    val mostPopular: (VertexId, (Int, Int)) = graph
      .aggregateMessages[(Int, Int)](
        edgeContext => {
          // msg_format: (outDegree: Int, numWords: Int)
          edgeContext.sendToSrc((1, sizeOf(edgeContext.srcAttr)))
        },
        (a, b) => (a._1 + b._1, a._2))
      .takeOrdered(1)(
        Ordering[(Int, Int)].reverse.on(_._2)
      )(0)

    val output: RDD[(VertexId, (Int, Int, List[String]))] = vertices
      .filter({
        case (vId, words) => vId == mostPopular._1
      })
      .map({
        // output_rdd: (vId: VertexId, (outDegree: Int, numWords: Int, words: List[String])
        case (vId, words) => (vId, (mostPopular._2._1, mostPopular._2._2, words))
      })

    output.saveAsTextFile(args(1) + TAG)

    println(s"The most popular vertex has ${mostPopular._2._1} edges.")

    sc.stop()
  }

  def sizeOf(attr: List[String]): Int = {
    if (attr != null) attr.size else 0
  }
}
