package edu.wisc.cs

import org.apache.spark.SparkContext
import org.apache.spark.graphx.{EdgeTriplet, _}
import org.apache.spark.storage.StorageLevel

/**
  * Created by shantanusinghal on 28/11/17 @ 2:46 AM.
  * NET-ID: singhal5
  * Campus ID: 9076101956
  *
  * PageRank using GraphX Pregel API
  */
object PartBApplication1Question1 {
  val TAG = "a1-q1/"
  val numIters = 20
  val resetProb = 0.15

  def main(args: Array[String]): Unit = {

    val sc = new SparkContext()

    val inputGraph: Graph[Int, Int] = GraphLoader.edgeListFile(
      sc,
      args(0),
      numEdgePartitions = -1,
      edgeStorageLevel = StorageLevel.MEMORY_AND_DISK_SER,
      vertexStorageLevel = StorageLevel.MEMORY_AND_DISK_SER)

    val outputGraph: Graph[Double, Double] = inputGraph
      .outerJoinVertices(inputGraph.outDegrees) {
        (vId, vAttr, outDeg) => outDeg.getOrElse(0)
      }
      .mapTriplets(triplet => 1.0 / triplet.srcAttr)
      .mapVertices((id, attr) => 1.0)
      .persist(StorageLevel.MEMORY_AND_DISK_SER)

    val rankGraph: Graph[Double, Double] =
      Pregel(outputGraph, 0.0, numIters, EdgeDirection.Out)(vertexProgram, sendMessage, messageCombiner)

    rankGraph.vertices.saveAsTextFile(args(1) + TAG)

    sc.stop()
  }

  def vertexProgram(id: VertexId, attr: Double, msgSum: Double): Double =
    resetProb + (1.0 - resetProb) * msgSum

  def sendMessage(edge: EdgeTriplet[Double, Double]): Iterator[(VertexId, Double)] =
    if (edge.srcAttr > 0.0001) Iterator((edge.dstId, edge.srcAttr * edge.attr)) else Iterator.empty

  def messageCombiner(a: Double, b: Double): Double = a + b

}
