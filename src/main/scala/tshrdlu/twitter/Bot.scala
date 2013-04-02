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
    stream.addListener(new UserStatusResponder(twitter))
    //FollowAnlp
    //println("Followed")
  }

  /**
   * Companion object for ReactiveBot with main method.
   */
  object ReactiveBot {

    def main(args: Array[String]) {

      val bot = new ReactiveBot
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
  class UserStatusResponder(twitter: Twitter) 
  extends StatusListenerAdaptor with UserStreamListenerAdaptor {

    import tshrdlu.util.SimpleTokenizer
    import collection.JavaConversions._

    val username = twitter.getScreenName

    lazy val stopwords = tshrdlu.util.English.stopwords_bot
    // Recognize a follow command
    lazy val FollowRE = """(?i)(?<=follow)(\s+(me|@[a-z_0-9]+))+""".r

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
   def FollowAnlp(){
       val followerIds = twitter.getFollowersIDs("appliednlp",-1).getIDs
      val anlp = followerIds.map{id => twitter.showUser(id)}.toSet
      val names = anlp.map{x => x.getScreenName}.toSet
     val anlp_names = names.filter{_.endsWith("_anlp")}
     anlp_names-"nrajani_anlp"
     println(anlp_names)
     val out = anlp_names.foreach(twitter.createFriendship)
    }
    /**
     * A method that possibly takes an action based on a status
     * it has received, and then produces a response.
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
        followSet.foreach(twitter.createFriendship)
        "OK. I FOLLOWED " + followSet.map("@"+_).mkString(" ") + "."  
      } else {
        
        try {
  	val StripLeadMentionRE(withoutMention) = text
   // println("making bigrams")
    val bigram = Tokenize(withoutMention).sliding(2).flatMap{case Vector(x,y) => List(x+" "+y)}.filterNot(z => (stopwords.contains(z(0))||stopwords.contains(z(1)))).toList.sortBy(_.length)
    //{case Vector(x,y) => List(x+" "+y)}
    val bigramsearch= bigram
        .toSet
        .takeRight(3)
        .toList
        .flatMap(w => twitter.search(new Query("\""+w+"\"")).getTweets)
        //bigramsearch.foreach(println)
  	/*val statusList = 
  	  SimpleTokenizer(withoutMention)
  	    .filter(_.length > 1)
  	    .filter(_.length < 10)
  	    .filterNot(_.contains('/'))
  	    .filter(tshrdlu.util.English.isSafe)
  	    .sortBy(- _.length)
  	    .toSet
  	    .take(3)
  	    .toList
  	    .flatMap(w => twitter.search(new Query(w)).getTweets) */
  	extractText(bigramsearch,bigram.toList)
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
    def extractText(statusList: List[Status],bigram:List[String]) = {
      //println("working")
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
      if (useableTweets.isEmpty) "NO." else
      { //var relevantTweets = Map[String,Int]()
        //for(i<- 0 to useableTweets.length-1){
        //val tweetBigram = Tokenize(useableTweets(i)).sliding(2).flatMap{case Vector(y,z) => List(y+" "+z)}
      // relevantTweets ++ tweetBigram.map(x => (x,i))
      //}
     // useableTweets.foreach(x => x.filter(y => stopwords.contains(y)) )
        val bigramMap = useableTweets.flatMap{x =>Tokenize(x).sliding(2).filterNot(z => (stopwords.contains(z(0))||stopwords.contains(z(1)))).map(bg => bg.mkString(" ") -> x)} toMap
        val sortedMap = bigramMap.filter{case (x,y) => !bigram.contains(x)}
        val mostRelevantTweet = sortedMap.groupBy(_._2).maxBy(_._2.size)
              //bigram collect { rb if bigram contains rb => bigramMap(rb) }
        //val relevantTweets = tweetBigram.map(x => (,x))
        /*for(j<- 0 to bigram.length-1){
        if(relevantTweets.contains(bigram(j))) 
          {
            val Some(num) = relevantTweets.get(bigram(j)) 
            useableTweets(num)
          }*/
          val found = for {
            rb <- bigram
            sentence <- bigramMap get rb
            } yield sentence
          //else useableTweets.head
      //}
     // sortedMap.foreach(println)
      bigramMap foreach {case (key, value) => println (key + "-->" + value)}
      //found.foreach(println)
      //found.headOption.getOrElse(useableTweets.head)
      mostRelevantTweet._1
      }
    }
    def Tokenize(text: String): IndexedSeq[String]={
      val starts = """(?:[#@])|\b(?:http)"""
      text
      .replaceAll("""([\?!()\";\|\[\].,':])""", " $1 ")
      .trim
      .split("\\s+")
      .toIndexedSeq
      .filterNot(x => x.startsWith(starts))
  }

  }

