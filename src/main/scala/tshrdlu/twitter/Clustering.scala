package tshrdlu.twitter

import twitter4j.Status
import nak.cluster._
import nak.util.CollectionUtil._
import chalk.util.SimpleTokenizer
import tshrdlu.util.English._

class StatusClusterer(
  pointCreator: StatusPointCreator = SimpleStatusPointCreator,
  distanceFnc: String = "cosine",
  transformerType: String = "pca"
) {

  import org.apache.log4j.Level
  import org.apache.log4j.Logger
  Logger.getRootLogger.setLevel(Level.INFO)

  def apply(statusList: Seq[Status], numClusters: Int) = {
    val points = pointCreator(statusList).toIndexedSeq
    val distance = DistanceFunction(distanceFnc)
    val transformer = PointTransformer(transformerType, points)
    val kmeans = new Kmeans(transformer(points), distance)
    val (dispersion, centroids) = kmeans.run(numClusters)
    val (_, predictedClusterIds) = kmeans.computeClusterMemberships(centroids)
    
    predictedClusterIds.zip(statusList.map(_.getText))
      .groupByKey
      .toSeq
      .sortBy(_._1)
  }
}

trait StatusPointCreator extends (Seq[Status] => Seq[Point])

object SimpleStatusPointCreator extends StatusPointCreator {

  def apply (statusList: Seq[Status]) = {

    val countsPerText = statusList.map { status => {
      SimpleTokenizer(status.getText)
        .map(_.toLowerCase)
	.filter(_.length > 2)
	.filterNot(stopwords)
	.counts
    }}

    val totalCounts = countsPerText.foldLeft(Map[String, Int]()) {
      (dfs, tcounts) =>
        dfs ++ tcounts.map { case (k, v) => k -> (v + dfs.getOrElse(k, 0)) }
    }

    val docFreqs = countsPerText
      .foldLeft(Map[String, Int]()) { (dfs, tcounts) =>
        dfs ++ tcounts.map { case (k, v) => k -> (1 + dfs.getOrElse(k, 0)) }
      }

    val tfidf = for ((k,v) <- totalCounts) yield (k,v.toDouble/docFreqs(k))

    val attributes = tfidf.toSeq.sortBy(_._2).takeRight(500).map(_._1).toSet

    //println(attributes.mkString(" "))
    val indices = attributes.toIndexedSeq.zipWithIndex.toMap
    val numDimensions = indices.size

    countsPerText.map { tcounts => {
      val coordinates = Array.fill(numDimensions)(0.0)
      for ((k,v) <- tcounts; if (attributes(k))) 
	coordinates(indices(k)) = v
      Point(coordinates.toIndexedSeq)
    }}

  }

}
