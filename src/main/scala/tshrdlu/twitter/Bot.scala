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
import collection.JavaConversions._

import main.scala.tshrdlu.twitter.TopicModeler



/**
 * Base trait with properties default for Configuration.
 * Gets a Twitter instance set up and ready to use.
 */
trait TwitterInstance {
  val twitter = new TwitterFactory().getInstance
  
}

/**
 * A bot that can monitor the stream and also take actions for the user.
 */
class ReactiveBot extends TwitterInstance with StreamInstance {
  val modeler = new TopicModeler("minTopicKeys.txt")
  stream.addListener(new UserStatusResponder(twitter,modeler))
  
}

/**
 * Companion object for ReactiveBot with main method.
 */
object ReactiveBot {
  
  def main(args: Array[String]) {
    if (args.length < 1) {
      println("Please input the path of the formatted topic words text file")
      System.exit(0)
    }
    val bot = new ReactiveBot

    //val modeler = new TopicModeler(args(0))
    
    bot.stream.user
    // If you aren't following a lot of users and want to get some
    // tweets to test out, use this instead of bot.stream.user.
    //bot.stream.sample
  }

}


/**
 * A listener that looks for messages to the user and replies using the
 * doActionGetReply method. Actions can be doing things like following,
 * and replies can be generated just about any way you'd like. The base
 * implementation searches for tweets on the API that have overlapping
 * vocabulary and replies with one of those.
 */
class UserStatusResponder(twitter: Twitter, modeler: TopicModeler) 
extends StatusListenerAdaptor with UserStreamListenerAdaptor {

  import tshrdlu.util.SimpleTokenizer
  import collection.JavaConversions._

  

  val username = twitter.getScreenName

  // Recognize a follow command
  lazy val FollowRE = """(?i)(?<=follow)(\s+(me|@[a-z]+))+""".r

  // Recognize a follow all _anlp users
  lazy val FollowAllANLPRE = """(follow anlp users)""".r
  
  // Pull just the lead mention from a tweet.
  lazy val StripLeadMentionRE = """(?:)^@[a-z_0-9]+\s(.*)$""".r

  // Pull the RT and mentions from the front of a tweet.
  lazy val StripMentionsRE = """(?:)(?:RT\s)?(?:(?:@[a-z]+\s))+(.*)$""".r   
  override def onStatus(status: Status) {
    println("New status: " + status.getText)
    
    val replyName = status.getInReplyToScreenName
    if (replyName == username) {
      println("*************")
      println("New reply: " + status.getText)
      val text = "@" + status.getUser.getScreenName + " " + doActionGetReply(status)
      println("Replying: " + text)
      val reply = new StatusUpdate(text).inReplyToStatusId(status.getId)
      twitter.updateStatus(reply)
    }
  }
 
  /**
   * A method that possibly takes an action based on a status
   * it has received, and then produces a response.
   */
  def doActionGetReply(status: Status) = {
    val text = status.getText.toLowerCase
    val followMatches = FollowRE.findAllIn(text)
    val followMatchesANLP = FollowAllANLPRE.findAllIn(text)
    if (!followMatchesANLP.isEmpty) {
      val cursor = -1
      val friendIDs = twitter.getFriendsIDs("appliednlp", cursor)

      val anlpStudentIDs = friendIDs.getIDs.filter{ id =>
        val user = twitter.showUser(id)
        user.getScreenName().endsWith("_anlp") && user.getScreenName() != "eric_anlp"
      }
      //anlpStudentIDs.foreach(println)

      anlpStudentIDs.foreach(twitter.createFriendship)
      "OK. I FOLLOWED THE STUDENTS IN THE ANLP CLASS."
    } else if (!followMatches.isEmpty) {
      val followSet = followMatches
	.next
	.drop(1)
	.split("\\s")
	.map {
	  case "me" => status.getUser.getScreenName
	  case screenName => screenName.drop(1)
	}
	.toSet
      followSet.foreach(twitter.createFriendship)
      "OK. I FOLLOWED " + followSet.map("@"+_).mkString(" ") + "."  
    } else {
      
      try {
	val StripLeadMentionRE(withoutMention) = text
        val statusTopicList = SimpleTokenizer(withoutMention)
			.filter(_.length > 4)
			.toSet
			.take(3)
			.toList
			.flatMap(w => modeler.wordTopicsMap.get(w))
			.flatten
	//statusTopicList.foreach(println)
	val topicWords:List[String] = statusTopicList.map(topic => modeler.topicWordsMap.getOrElse(topic,Set(" "))).flatten
	val statusQueryList = 
		topicWords
		.filter(_.length > 4)
                .filter(_.length < 11)
	        .sortBy(- _.length)
		.toSet
		.take(4)
		.toList
	statusQueryList.foreach(println)

	// search for tweets based on words from same topic as original tweet
        val statusList = statusQueryList.flatMap(w => twitter.search(new Query(w)).getTweets)

	
	// Original statusList
	/*val statusList = 
	  SimpleTokenizer(withoutMention)
	    .filter(_.length > 3)
	    .filter(_.length < 10)
	    .filterNot(_.contains('/'))
	    .filter(tshrdlu.util.English.isSafe)
	    .sortBy(- _.length)
	    .toSet
	    .take(3)
	    .toList
	    .flatMap(w => twitter.search(new Query(w)).getTweets)*/
	
	extractText(statusList,statusTopicList)
      }	catch { 
	case _: Throwable => "NO."
      }
    }
  
  }

  /**
   * Go through the list of Statuses, filter out the non-English ones and
   * any that contain (known) vulgar terms, strip mentions from the front,
   * filter any that have remaining mentions or links, and then return the
   * head of the set, if it exists.
   */
  def extractText(statusList: List[Status], statusTopics: List[String]) = {
    val useableTweets = statusList
      .map(_.getText)
      .map {
	case StripMentionsRE(rest) => rest
	case x => x
      }
      .filterNot(_.contains('@'))
      .filterNot(_.contains('/'))
      .filter(tshrdlu.util.English.isEnglish)
      .filter(tshrdlu.util.English.isSafe)
    
    //Use topic model to select response
    val topicDistributions = for ( tweet <- useableTweets) yield {
    				SimpleTokenizer(tweet).filter(_.length > 4)
				.toSet
				.take(3)
				.toList
				.flatMap(w => modeler.wordTopicsMap.get(w))
				.flatten}
    val topicSimilarity = topicDistributions.map(ids => ids.intersect(statusTopics).size)
    val indexMax = topicSimilarity.toList.zipWithIndex.maxBy(_._1)._2
    
    if (useableTweets.isEmpty) "NO." else useableTweets(indexMax)
  }

}
