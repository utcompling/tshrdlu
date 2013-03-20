package tshrdlu.twitter.index

/**
 * Copyright 2013 Nick Wilson
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

import twitter4j.{Status,TwitterStreamFactory}

import tshrdlu.twitter.{Filterable,StatusListenerAdaptor}
import tshrdlu.twitter.{IdFilter,LocationFilter,TermFilter}
import tshrdlu.util.English
import tshrdlu.util.index._


/**
 * Provides a main method to perform searches on indexes.
 */
object StatusSearcher extends ObjectIndexSearcher[Status](StatusReaderFactory) {
  def resultToString(status: Status, score: Float): String = {
    score + ": " + status.getText
  }
}


/**
 * Handles incoming safe English status messages by indexing them. Accepts
 * incoming statuses, verifies they are in English and are "safe", writes them
 * to an index, and prints the status text.
 */
class SafeEnglishStatusIndexListener extends StatusListenerAdaptor {
  private var writer: ObjectWriter[Status] = null

  override def onStatus(status: Status) {
    require(writer != null)

    val text = status.getText
    if (English.isEnglish(text) && English.isSafe(text)) {
      writer.add(status)
      println(text)
    }
  }

  /**
   * Set the writer that contains everything needed to write to an index.
   *
   * @param statusWriter the index writer
   */
  def setStatusWriter(statusWriter: ObjectWriter[Status]) {
    writer = statusWriter
  }
}


class BaseSafeEnglishStatusIndexStreamer extends SafeEnglishStatusIndexListener {
  val twitterStream = new TwitterStreamFactory().getInstance
  twitterStream.addListener(this)

  /**
   * Samples statuses and writes them to an index.
   *
   * @param args takes one argument, a name of an index (see
   *             [[tshrdlu.util.index.Settings.BaseIndexPath]] for a
   *             decription of where the index is located on disk) or a full
   *             path to an index
   */
  def main(args: Array[String]) {
    setStatusWriter(StatusWriterFactory(args(0)))
    twitterStream.sample
  }
}


/**
 * Takes safe English status messages from the Twiter sample stream
 * and indexes them. Accepts incoming statuses, verifies they are in English
 * and are "safe", writes them to an index, and prints the status text.
 */
object SafeEnglishStatusIndexer extends BaseSafeEnglishStatusIndexStreamer


abstract class FilteredSafeEnglishStatusIndexStreamer
extends BaseSafeEnglishStatusIndexStreamer with Filterable {

  /**
   * Get a filtered safe English stream based on the FilterQuery provided by
   * extending classes.
   *
   * @param args the first argument is a name of an index (see
   *             [[tshrdlu.util.index.Settings.BaseIndexPath]] for a
   *             decription of where the index is located on disk) or a full
   *             path to an index. All remaining arguments are used as the
   *             filter query.
   */
  override def main(args: Array[String]) {
    setStatusWriter(StatusWriterFactory(args(0)))

    val queryArgs = args.slice(1, args.length)
    twitterStream.filter(getQuery(queryArgs))
  }
}


/**
 * An indexer that indexes safe English tweets based on the provided user ids.
 */
object SafeEnglishStatusIdIndexer
extends FilteredSafeEnglishStatusIndexStreamer with IdFilter


/**
 * An indexer that indexes safe English tweets based on the provided search
 * terms.
 */
object SafeEnglishStatusTermIndexer
extends FilteredSafeEnglishStatusIndexStreamer with TermFilter


/**
 * An indexer that indexes safe English tweets from locations based on the
 * provided bounding boxes.
 */
object SafeEnglishStatusLocationIndexer
extends FilteredSafeEnglishStatusIndexStreamer with LocationFilter
