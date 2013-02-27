package tshrdlu.twitter

/**
 * Copyright 2013 Jason Baldridge
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import twitter4j.TwitterStreamFactory

/**
 * Extend this to have a stream that is configured and ready to use.
 */
trait StreamInstance {
  val stream = new TwitterStreamFactory().getInstance
}

/**
 * An example of how to obtain a certain number of tweets from the
 * sample stream and cluster them.
 */
object ClusterStream extends StreamInstance {

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
