package edu.wisc.cs

import org.apache.spark.SparkContext
import org.apache.spark.graphx._
import org.apache.spark.rdd.RDD

/**
  * Created by shantanusinghal on 29/11/17 @ 2:40 AM.
  * NET-ID: singhal5
  * Campus ID: 9076101956
  *
  * Question 3 - average word count in neighbourhood
  */
object PartBApplication2Question3 {
  val TAG = "a2-q3/"

  def main(args: Array[String]): Unit = {

    val sc = new SparkContext()

    val inputFile = sc.textFile(args(0))

    val (vertices, edges) = GraphUtils.parse(inputFile)

    val graph: Graph[List[String], String] = Graph(vertices, edges).cache()

    val output: RDD[(VertexId, Double)] = graph
      .aggregateMessages[Map[VertexId, Int]](
        edgeContext => {
          // msg_format: Map[vId: VertexId, numWords: Int]
          edgeContext.sendToSrc(Map(edgeContext.dstId -> edgeContext.dstAttr.size))
        },
        (a, b) => a.++(b))
      .map({
        case (vId, msg) => (vId, totalWordsIn(msg) / totalNeighboursIn(msg))
      })

    output.saveAsTextFile(args(1) + TAG)

    println(s"View the list of average number of words in their neighborhood of each vertex at ${args(1) + TAG}")

    sc.stop()
  }

  def totalNeighboursIn(map: Map[VertexId, Int]): Double = {
    map.size.toDouble
  }

  def totalWordsIn(map: Map[VertexId, Int]): Double = {
    map.values.sum.toDouble
  }
}
