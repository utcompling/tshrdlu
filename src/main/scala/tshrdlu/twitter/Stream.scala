package tshrdlu.twitter

import twitter4j.TwitterStreamFactory
import twitter4j.conf.Configuration
import TwitterAuthentication._

/**
 * Extend this to have a stream that is configured and ready to use.
 */
trait BaseStream {
  val stream = new TwitterStreamFactory(configFromProperties).getInstance
}

/**
 * An example of how to obtain a certain number of tweets from the
 * sample stream and cluster them.
 */
object ClusterStream extends BaseStream {

  val engTweets = new EnglishStatusAccumulator

  def main(args: Array[String]) {
    val Array(numClusters, numTweets) = args.map(_.toInt)

    stream.addListener(engTweets)

    println("Collecting " + numTweets + " tweets.")
    stream.sample

    while (engTweets.count < numTweets) { Thread.sleep(1) }
    stream.shutdown

    println("Running kmeans.")
    val clustered = new StatusClusterer()(engTweets.tweets, numClusters)

    println("Examples from the clusters found.\n")
    for ((id,cluster) <- clustered) {
      println("Cluster " + id)
      cluster.take(5).foreach(println)
      println
    }
  }

}
