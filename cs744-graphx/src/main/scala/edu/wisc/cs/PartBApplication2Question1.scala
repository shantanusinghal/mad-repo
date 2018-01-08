package edu.wisc.cs

import org.apache.spark.SparkContext
import org.apache.spark.graphx._

/**
  * Created by shantanusinghal on 29/11/17 @ 2:40 AM.
  * NET-ID: singhal5
  * Campus ID: 9076101956
  *
  * Question 1 - edges where source has more words
  */
object PartBApplication2Question1 {
  val TAG = "a2-q1/"

  def main(args: Array[String]): Unit = {

    val sc = new SparkContext()

    val inputFile = sc.textFile(args(0))

    val (vertices, edges) = GraphUtils.parse(inputFile)

    val graph: Graph[List[String], String] = Graph(vertices, edges).cache()

    val output: Long = graph
      .triplets
      .filter(triplet => {
        sizeOf(triplet.srcAttr) > sizeOf(triplet.dstAttr)
      })
      .count()

    println(s"There are $output edges where the number of words in the source vertex is strictly larger than the number of words in the destination vertex.")

    sc.stop()
  }

  def sizeOf(attr: List[String]): Int = {
    if (attr != null) attr.size else 0
  }
}
