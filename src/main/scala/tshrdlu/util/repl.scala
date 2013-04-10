package tshrdlu.repl

import akka.actor._
import twitter4j._
import akka.pattern.ask
import akka.util._
import tshrdlu.twitter.Bot._

object ReplBot {
  var bot: ActorRef = null

  def setup {
    val system = ActorSystem("Repl")
    bot = system.actorOf(Props[ReplBot], name = "ReplBot")
  }
  
  def loadReplier(name: String) { bot ! ReplierByName(name) }
  
  def sendTweet(text: String) { bot ! FakeStatus(text) }
  def sendTweet(name: String, text: String) { bot ! FakeStatus(text, name) }
  def sendTweet(screenName: String, name: String, text: String) { bot ! FakeStatus(text, FakeUser(name, screenName)) }
}

class ReplBot extends tshrdlu.twitter.Bot {

  override def preStart {
    // deactivate loading of all
  }

  def replReceive: Receive = {
    case UpdateStatus(update) => log.info(s"got update: $update")
  }
  override def receive: Receive = replReceive.orElse(super.receive)
}

object FakeStatus {
  val random = new java.util.Random
  // Maybe implicits?
  def apply(id: Long, text: String, user: String): FakeStatus = apply(id, text, FakeUser(user))
  def apply(text: String, user: String): FakeStatus = apply(random.nextInt, text, user)
  def apply(text: String, user: User): FakeStatus = apply(random.nextInt, text, user)
  def apply(id: Long, text: String, user: User): FakeStatus = new FakeStatus(id, text, user)
  def apply(text: String): FakeStatus = apply(text, "thedoctor")
}

// Add further fields via accessors.
class FakeStatus(id: Long, text: String, user: User) extends Status {
  // Members declared in java.lang.Comparable
  def compareTo(other: twitter4j.Status): Int = Integer.valueOf((getId - other.getId).intValue())

  // Members declared in twitter4j.EntitySupport
  def getHashtagEntities(): Array[twitter4j.HashtagEntity] = ???
  def getMediaEntities(): Array[twitter4j.MediaEntity] = ???
  def getURLEntities(): Array[twitter4j.URLEntity] = ???
  def getUserMentionEntities(): Array[twitter4j.UserMentionEntity] = ???

  // Members declared in twitter4j.Status
  def getContributors(): Array[Long] = ???
  def getCreatedAt(): java.util.Date = ???
  def getCurrentUserRetweetId(): Long = ???
  def getGeoLocation(): twitter4j.GeoLocation = ???
  def getId(): Long = id
  def getInReplyToScreenName(): String = new TwitterStreamFactory().getInstance.getScreenName
  def getInReplyToStatusId(): Long = ???
  def getInReplyToUserId(): Long = ???
  def getPlace(): twitter4j.Place = ???
  def getRetweetCount(): Long = ???
  def getRetweetedStatus(): twitter4j.Status = ???
  def getSource(): String = ???
  def getText(): String = text
  def getUser(): twitter4j.User = user
  def isFavorited(): Boolean = ???
  def isPossiblySensitive(): Boolean = ???
  def isRetweet(): Boolean = ???
  def isRetweetedByMe(): Boolean = ???
  def isTruncated(): Boolean = ???

  // Members declared in twitter4j.TwitterResponse
  def getAccessLevel(): Int = ???
  def getRateLimitStatus(): twitter4j.RateLimitStatus = ???
}

object FakeUser {
  val random = new java.util.Random
  def apply(id: Long, name: String, screenName: String): FakeUser = new FakeUser(id, name, screenName)
  def apply(name: String, screenName: String): FakeUser = apply(random.nextInt, name, screenName)
  def apply(name: String): FakeUser = apply(random.nextInt, name, name)
}

class FakeUser(id: Long, name: String, screenName: String) extends User {
  // Members declared in java.lang.Comparable
  def compareTo(other: twitter4j.User): Int = Integer.valueOf((getId - other.getId).intValue())

  // Members declared in twitter4j.TwitterResponse
  def getAccessLevel(): Int = ???
  def getRateLimitStatus(): twitter4j.RateLimitStatus = ???

  // Members declared in twitter4j.User
  def getBiggerProfileImageURL(): String = ???
  def getBiggerProfileImageURLHttps(): String = ???
  def getCreatedAt(): java.util.Date = ???
  def getDescription(): String = ???
  def getDescriptionURLEntities(): Array[twitter4j.URLEntity] = ???
  def getFavouritesCount(): Int = ???
  def getFollowersCount(): Int = ???
  def getFriendsCount(): Int = ???
  def getId(): Long = id
  def getLang(): String = ???
  def getListedCount(): Int = ???
  def getLocation(): String = ???
  def getMiniProfileImageURL(): String = ???
  def getMiniProfileImageURLHttps(): String = ???
  def getName(): String = name
  def getOriginalProfileImageURL(): String = ???
  def getOriginalProfileImageURLHttps(): String = ???
  def getProfileBackgroundColor(): String = ???
  def getProfileBackgroundImageURL(): String = ???
  def getProfileBackgroundImageUrl(): String = ???
  def getProfileBackgroundImageUrlHttps(): String = ???
  def getProfileBannerIPadRetinaURL(): String = ???
  def getProfileBannerIPadURL(): String = ???
  def getProfileBannerMobileRetinaURL(): String = ???
  def getProfileBannerMobileURL(): String = ???
  def getProfileBannerRetinaURL(): String = ???
  def getProfileBannerURL(): String = ???
  def getProfileImageURL(): String = ???
  def getProfileImageURLHttps(): String = ???
  def getProfileImageUrlHttps(): java.net.URL = ???
  def getProfileLinkColor(): String = ???
  def getProfileSidebarBorderColor(): String = ???
  def getProfileSidebarFillColor(): String = ???
  def getProfileTextColor(): String = ???
  def getScreenName(): String = screenName
  def getStatus(): twitter4j.Status = ???
  def getStatusesCount(): Int = ???
  def getTimeZone(): String = ???
  def getURL(): String = ???
  def getURLEntity(): twitter4j.URLEntity = ???
  def getUtcOffset(): Int = ???
  def isContributorsEnabled(): Boolean = ???
  def isFollowRequestSent(): Boolean = ???
  def isGeoEnabled(): Boolean = ???
  def isProfileBackgroundTiled(): Boolean = ???
  def isProfileUseBackgroundImage(): Boolean = ???
  def isProtected(): Boolean = ???
  def isShowAllInlineMedia(): Boolean = ???
  def isTranslator(): Boolean = ???
  def isVerified(): Boolean = ???
}
