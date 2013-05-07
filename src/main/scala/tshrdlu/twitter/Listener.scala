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
import tshrdlu.util.English

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
trait StatusListenerAdaptor extends StatusListener {
  def onStatus(status: Status) {}
  def onDeletionNotice(statusDeletionNotice: StatusDeletionNotice) {}
  def onTrackLimitationNotice(numberOfLimitedStatuses: Int) {}
  def onException(ex: Exception) { }
  def onScrubGeo(arg0: Long, arg1: Long) {}
  def onStallWarning(warning: StallWarning) {}
}

/**
 * A listener that prints the text of each status to standard out.
 */
class PrintStatusListener extends StatusListenerAdaptor {
  override def onStatus(status: Status) { 
    println(status.getText) 
  }
}

/**
 * A listener that prints the text of each status to standard out.
 */
trait EnglishStatusListener extends StatusListenerAdaptor {
  override def onStatus(status: Status) {
    if(English.isEnglish(status.getText)) println(status.getText)
  }
}

/**
 * A status listener that identifies admissable tweets
 * and accumulates them.
 */ 
trait StatusAccumulator extends StatusListenerAdaptor {

  /**
   * A function that, given the text of a tweet, says whether it
   * is something to accumulate or not.
   */
  def admissable: (String => Boolean)

  /**
   * Return the number of tweets accumulated so far.
   */
  def count = numTweetsSeen

  // The counter
  private var numTweetsSeen = 0

  /**
   * Return tweets accumulated so far.
   */
  def tweets = tweetBuffer.toSeq

  // The accumulator
  private val tweetBuffer = collection.mutable.ListBuffer[Status]()

  override def onStatus(status: Status) {
    if (admissable(status.getText)) {
      tweetBuffer += status
      numTweetsSeen += 1
    }
  }

}

/**
 * Accumulate English tweets using the not-so-good isEnglish method
 * in tshrdlu.util.English.
 */
class EnglishStatusAccumulator extends StatusAccumulator {
  def admissable = tshrdlu.util.English.isEnglish
}


trait UserStreamListenerAdaptor extends UserStreamListener {

  def onBlock(source: User, blockedUser: User) {}
  def onDeletionNotice(directMessageId: Long, userId: Long) {}
  def onDirectMessage(directMessage: DirectMessage) {}
  def onFavorite(source: User, target: User, favoritedStatus: Status) {}
  def onFollow(source: User, followedUser: User) {}
  def onFriendList(friendIds: Array[Long]) {}
  def onUnblock(source: User, unblockedUser: User) {}
  def onUnfavorite(source: User, target: User, unfavoritedStatus: Status) {}
  def onUserListCreation(listOwner: User, list: UserList) {}
  def onUserListDeletion(listOwner: User, list: UserList) {}
  def onUserListMemberAddition(addedMember: User, listOwner: User, list: UserList) {}
  def onUserListMemberDeletion(deletedMember: User, listOwner: User, list: UserList) {}
  def onUserListSubscription(subscriber: User, listOwner: User, list: UserList){} 
  def onUserListUnsubscription(subscriber: User, listOwner: User, list: UserList) {}
  def onUserListUpdate(listOwner: User, list: UserList) {}
  def onUserProfileUpdate(updatedUser: User) {}
}

