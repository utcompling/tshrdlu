package main.scala.tshrdlu.twitter

import io.Source

/**
 * Created with IntelliJ IDEA.
 * User: eric
 * Date: 3/7/13
 * Time: 9:27 AM
 * To change this template use File | Settings | File Templates.
 */
object InputReader {
  def main(args: Array[String]) {
    if (args.length < 1) {
      print("Please input the name of the input file.")
      System.exit(0)
    }

    val inputIterator = Source.fromFile(args(0)).getLines.toList

    val tuples = inputIterator.map { line =>
      val split = line.split(" ")
      (split(0) -> split(1))
    }

    val wordTopicsMap = tuples.groupBy(e => e._1).mapValues(e => e.map(x => x._2).toSet)

    val tuplesRev = inputIterator.map { line =>
      val split = line.split(" ")
      (split(1) -> split(0))
    }

    val topicWordsMap = tuplesRev.groupBy(e => e._1).mapValues(e => e.map(x => x._2).toSet)
  }
}
