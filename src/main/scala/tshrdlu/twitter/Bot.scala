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
import collection.JavaConversions._

/**
 * Base trait with properties default for Configuration.
 * Gets a Twitter instance set up and ready to use.
 */
trait BaseUser {
  val user = new TwitterFactory(configFromProperties).getInstance
}


/**
 * A bot that can monitor the stream and also take actions for the user.
 */
class ReactiveUser extends BaseUser with BaseStream {
  stream.addListener(new UserStatusResponder(user))
}

/**
 * Companion object for ReactiveUser with main method.
 */
object ReactiveUser {

  def main(args: Array[String]) {
    val bot = new ReactiveUser
    //bot.stream.sample
    bot.stream.user
  }

}


/**
 * A listener that looks for messages to the user and replies using the
 * doActionGetReply method. Actions can be doing things like following,
 * and replies can be generated just about any way you'd like. The base
 * implementation searches for tweets on the API that have overlapping
 * vocabulary and replies with one of those.
 */
class UserStatusResponder(user: Twitter) extends StatusListenerAdaptor with UserStreamListenerAdaptor {

  import chalk.util.SimpleTokenizer
  import collection.JavaConversions._

  val username = user.getScreenName
  
  override def onStatus(status: Status) {
    println("New status: " + status.getText)
    val replyName = status.getInReplyToScreenName
    if (replyName == username) {
      println("*************")
      println("New reply: " + status.getText)
      val text = "@" + status.getUser.getScreenName + " " + doActionGetReply(status)
      println("Replying: " + text)
      val reply = new StatusUpdate(text).inReplyToStatusId(status.getId)
      user.updateStatus(reply)
    }
  }

  // Recognize a follow command
  lazy val FollowRE = """(?i)(?<=follow)(\s+(me|@[a-z]+))+""".r

  // Pull just the lead mention from a tweet.
  lazy val StripLeadMentionRE = """(?:)^@[a-z]+\s(.*)$""".r

  // Pull the RT and mentions from the front of a tweet.
  lazy val StripMentionsRE = """(?:)(?:RT\s)?(?:(?:@[a-z]+\s))+(.*)$""".r  


  /**
   * A method that possibly takes an action based 
   */
  def doActionGetReply(status: Status) = {
    val text = status.getText.toLowerCase
    val followMatches = FollowRE.findAllIn(text)
    if (!followMatches.isEmpty) {
      val followSet = followMatches
	.next
	.drop(1)
	.split("\\s")
	.map {
	  case "me" => status.getUser.getScreenName
	  case screenName => screenName.drop(1)
	}
	.toSet
      followSet.foreach(user.createFriendship)
      "OK. I FOLLOWED " + followSet.map("@"+_).mkString(" ") + "."  
    } else {
      
      try {
	val StripLeadMentionRE(withoutMention) = text
	val statusList = 
	  SimpleTokenizer(withoutMention)
	    .filter(_.length > 3)
	    .toSet
	    .take(3)
	    .toList
	    .flatMap(w => user.search(new Query(w)).getTweets)
	extractText(statusList)
      }	catch { 
	case _: Throwable => "NO."
      }
    }
  
  }

  /**
   * Go through the list of Statuses, filter out the non-English ones,
   * strip mentions from the front, filter any that have remaining
   * mentions, and then return the head of the set, if it exists.
   */
  def extractText(statusList: List[Status]) = {
    val useableTweets = statusList
      .map(_.getText)
      .map {
	case StripMentionsRE(rest) => rest
	case x => x
      }
      .filterNot(_.contains('@'))
      .filter(tshrdlu.util.English.isEnglish)

    if (useableTweets.isEmpty) "NO." else useableTweets.head
  }

}



object TestUser extends BaseUser {
  def main(args: Array[String]) { 

    println(user.getRateLimitStatus("application"))

    val query = new Query("scala")
    val result = user.search(query)
    result.getTweets.foreach(t => println(t.getUser.getScreenName + ": " + t.getText))

    //user.getHomeTimeline.take(3).foreach(status => println(status.getText))

    //user.updateStatus(new StatusUpdate("OK."))

    //user.getMentionsTimeline.foreach { status => {
    //  //println(status.getUserMentionEntities.map(_.getScreenName).mkString(" "))
    //  println(status.getId)
    //  val participants = 
    //	(status.getUser.getScreenName :: status.getUserMentionEntities.map(_.getScreenName).toList).toSet - userName
    //  
    //  val text = participants.map(p=>"@"+p).mkString(" ") + " OK."
    //  val reply = new StatusUpdate(text).inReplyToStatusId(status.getId)
    //  println("Replying: " + text)
    //  user.updateStatus(reply)
    //}}


    //val anlpFollowers = user.getFollowersIDs("appliednlp",-1).getIDs
    //anlpFollowers.foreach { id => {
    //  println("Following: " + user.showUser(id).getName)
    //  user.createFriendship(id)
    //}}

    //var count = 0
    //val jmbFollowers = user.getFollowersIDs("jasonbaldridge",-1).getIDs
    //jmbFollowers.foreach { id => {
    //  println("Following: " + user.showUser(id).getScreenName)
    //  count += 1
    //  println(count)
    //  //user.createFriendship(id)
    //}}

    
    //val friends = user.getFriendsIDs(-1).getIDs
    //friends.foreach { friendId => {
    //  val user = user.showUser(friendId)
    //  val status = user.getStatus
    //  val text = if (status==null) "" else status.getText
    //  println(user.getName + ": " + text)
    //  
    //  if (status!=null) {
    //	user.retweetStatus(status.getId)
    //  }
    //}}

  }
}
