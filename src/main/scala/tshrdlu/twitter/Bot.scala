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

import akka.actor._
import twitter4j._
import collection.JavaConversions._

/**
 * The main actor for a Bot, which basically performance the actions that a person
 * might do as an active Twitter user.
 */
class Bot extends Actor with ActorLogging {
  import Bot._

  val userStream = context.actorOf(Props[UserStreamDispatcher], name = "UserStream")
  val twitter = new TwitterFactory().getInstance
  val simpleReplier = context.actorOf(Props[SimpleReplyCreator], name = "SimpleReplier")

  def receive = {
    case Start => 
      userStream ! RegisterReplier(simpleReplier)
      userStream ! MonitorUserStream(true)
      userStream ! MonitorUserStream(false)
      userStream ! MonitorUserStream(true)

    case Shutdown => userStream ! Shutdown

    case SearchTwitter(query) => 
      val tweets: Seq[Status] = twitter.search(query).getTweets.toSeq
      sender ! tweets
      
    case UpdateStatus(update) => 
      log.info("Posting update: " + update.getStatus)
      twitter.updateStatus(update)
  }
}

/**
 * An object to define the message types that the actors in the bot use for
 * communication.
 *
 * Also provides the main method for starting up the bot. No configuration
 * currently supported.
 */
object Bot {
  
  object Start
  object Shutdown
  case class MonitorUserStream(listen: Boolean)
  case class RegisterReplier(replier: ActorRef)
  case class ReplyToStatus(status: Status)
  case class SearchTwitter(query: Query)
  case class UpdateStatus(update: StatusUpdate)

  def main (args: Array[String]) {
    val system = ActorSystem("TwitterBot")
    val bot = system.actorOf(Props[Bot], name = "Bot")
    bot ! Start
  }

}

/**
 * An actor that monitors the user stream and dispatches events to the
 * appropriate actors that have been registered with it. Currently only
 * attends to updates that are addressed to the user account.
 */
class UserStreamDispatcher extends Actor 
with StatusListenerAdaptor with UserStreamListenerAdaptor with ActorLogging {
  import Bot._

  val stream = new TwitterStreamFactory().getInstance
  val username = stream.getScreenName

  stream.addListener(this)

  var repliers = Vector.empty[ActorRef]

  def receive = {
    case MonitorUserStream(listen) => 
      if (listen) stream.user else stream.shutdown

    case RegisterReplier(replier) => repliers = repliers :+ replier
  }

  override def onStatus(status: Status) {
    log.info("New status: " + status.getText)
    val replyName = status.getInReplyToScreenName
    if (replyName == username) {
      log.info("Replying to: " + status.getText)
      repliers.foreach(_ ! ReplyToStatus(status))
    }
  }

}


/**
 * An actor that constructs replies to a given status.
 */
class SimpleReplyCreator extends Actor with ActorLogging {
  import Bot._
  import TwitterRegex._
  import tshrdlu.util.SimpleTokenizer

  import context.dispatcher
  import akka.pattern.ask
  import akka.util._
  import scala.concurrent.duration._
  import scala.concurrent.{Future,Await}
  implicit val timeout = Timeout(10 seconds)

  def receive = {
    case ReplyToStatus(status) => 
      log.info("Replying to: " + status.getText)
      val replyName = status.getUser.getScreenName
      
      val candidatesFuture = getReplies(status, 139-replyName.length)
      candidatesFuture onSuccess {
        case candidates => 
          val reply = "@" + replyName + " " + candidates.toSet.head
          context.parent ! UpdateStatus(new StatusUpdate(reply).inReplyToStatusId(status.getId))
      }
  }
  
  /**
   * Produce a reply to a status.
   */
  def getReplies(status: Status, maxLength: Int = 140): Future[Seq[String]] = {
    val text = status.getText.toLowerCase match {
      case StripLeadMentionRE(withoutMention) => withoutMention
      case x => x
    }
    
    // Get a sequence of futures of status sequences (one seq for each query)
    val statusSeqFutures: Seq[Future[Seq[Status]]] = SimpleTokenizer(text)
      .filter(_.length > 3)
      .filter(_.length < 10)
      .filterNot(_.contains('/'))
      .filter(tshrdlu.util.English.isSafe)
      .sortBy(- _.length)
      .take(3)
      .map(w => (context.parent ? SearchTwitter(new Query(w))).mapTo[Seq[Status]])

    // Convert this to a Future of a single sequence of candidate replies
    val statusesFuture: Future[Seq[Status]] = 
      Future.sequence(statusSeqFutures).map(_.flatten).mapTo[Seq[Status]]

    // Filter statuses to their text and make sure they are short enough to use.
    statusesFuture.map(_.flatMap(getText).filter(_.length <= maxLength))
  }

  /**
   * Go through the list of Statuses, filter out the non-English ones and
   * any that contain (known) vulgar terms, strip mentions from the front,
   * filter any that have remaining mentions or links, and then return the
   * head of the set, if it exists.
   */
  def getText(status: Status): Option[String] = {
    import tshrdlu.util.English.{isEnglish,isSafe}

    val text = status.getText match {
      case StripMentionsRE(rest) => rest
      case x => x
    }
    
    if (!text.contains('@') && !text.contains('/') && isEnglish(text) && isSafe(text))
      Some(text)
    else None
  }

}


object TwitterRegex {

  // Recognize a follow command
  lazy val FollowRE = """(?i)(?<=follow)(\s+(me|@[a-z_0-9]+))+""".r

  // Pull just the lead mention from a tweet.
  lazy val StripLeadMentionRE = """(?:)^@[a-z_0-9]+\s(.*)$""".r

  // Pull the RT and mentions from the front of a tweet.
  lazy val StripMentionsRE = """(?:)(?:RT\s)?(?:(?:@[a-z]+\s))+(.*)$""".r   


}
