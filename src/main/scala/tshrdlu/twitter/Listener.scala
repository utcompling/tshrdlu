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

/**
 * An adaptor that provides dummy implementations of all
 * StatusListener methods other than onStatus so that this
 * adaptor can be extended by multiple classes that are
 * also going to ignore all these methods. (Though they can
 * of course override any they need to handle.)
 *
 * This is of course not an endorsement that this is good
 * practice -- it is just an expedient for working on example
 * code that isn't doing any storing of tweets or processing
 * massive amounts of them.
 */
abstract class StatusOnlyListener extends StatusListener {
  def onDeletionNotice(statusDeletionNotice: StatusDeletionNotice) {}
  def onTrackLimitationNotice(numberOfLimitedStatuses: Int) {}
  def onException(ex: Exception) { ex.printStackTrace }
  def onScrubGeo(arg0: Long, arg1: Long) {}
  def onStallWarning(warning: StallWarning) {}
}

/**
 * A listener that prints the text of each status to standard out.
 */
class PrintStatusListener extends StatusOnlyListener {
  def onStatus(status: Status) { 
    println(status.getText) 
  }
}
