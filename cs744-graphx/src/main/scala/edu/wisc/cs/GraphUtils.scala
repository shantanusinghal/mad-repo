package edu.wisc.cs

import org.apache.spark.graphx.{Edge, _}
import org.apache.spark.rdd.RDD

/**
  * Created by shantanusinghal on 02/12/17 @ 8:49 PM.
  * NET-ID: singhal5
  * Campus ID: 9076101956
  */
object GraphUtils {

  /**
    * @param file output of Question 2 from Assignment 2 - Part B
    * @return property graph
    */
  def parse(file: RDD[String]): (RDD[(VertexId, List[String])], RDD[Edge[String]]) = {
    // Parse each line to create list of vertices with list of words
    val vertices: RDD[(VertexId, List[String])] = file
      .map( line => {
        line.split(",").transform(_.trim).toList
      })
      .filter(_.nonEmpty)
      .map( wordList => {
        (nextId, wordList)
      })
      .persist()

    // Flatten the vertices and Join to get the common-word Edges
    val vertices_flattened: RDD[(String, VertexId)] = vertices
      .flatMap({
        case (id, words) => words.map((_, id))
      })
    val edges: RDD[Edge[String]] = vertices_flattened
      .join(vertices_flattened)
      .map({
        case (word, (srcIc, destId)) => new Edge[String](srcIc, destId, word)
      })
      .filter({
        case Edge(srcId, destId, word) => srcId != destId
      })

    (vertices, edges)

  }

  def nextId: Long = {
    scala.util.Random.nextInt(Int.MaxValue).toLong
  }

}
