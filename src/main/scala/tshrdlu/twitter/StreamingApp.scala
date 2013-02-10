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

import twitter4j._
import twitter4j.conf.Configuration
import TwitterAuthentication._

/**
 * Base class that sets reasonable defaults for Configuration
 * and StatusListener and gets a TwitterStream instance set up and
 * ready to use.
 */
class BaseStreamer(config: Configuration = configFromProperties)
extends PrintStatusListener {
  val twitterStream = new TwitterStreamFactory(config).getInstance
  twitterStream.addListener(this)

  /**
   * Do something interesting with the stream. Base implementation
   * displays tweets from the random sample.
   */
  def main(args: Array[String]) { 
    twitterStream.sample 
  }
}

/**
 * An object that uses BaseStreamer's defaults and main method.
 */
object StatusStreamer extends BaseStreamer

/**
 * A base class for streamers that use a filter.
 */
abstract class FilteredStreamer extends BaseStreamer with Filterable {

  /**
   * Get a filtered stream based on the FilterQuery provided by
   * extending classes.
   */
  override def main(args: Array[String]) {
    twitterStream.filter(getQuery(args))
  }

}

/**
 * A streamer that displays tweets based on the provided user ids.
 */
object IdStreamer extends FilteredStreamer with IdFilter

/**
 * A streamer that displays tweets based on the provided query terms.
 */
object TermStreamer extends FilteredStreamer with TermFilter

/**
 * A streamer that displays tweets for given locations based on the
 * provided bounding boxes.
 */
object LocationStreamer extends FilteredStreamer with LocationFilter

