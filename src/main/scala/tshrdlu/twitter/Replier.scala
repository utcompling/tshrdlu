package tshrdlu.twitter

import akka.actor._
import twitter4j._

/**
 * An actor that constructs replies to a given status.
 */
trait BaseReplier extends Actor with ActorLogging {
  import Bot._
  import TwitterRegex._
  import tshrdlu.util.SimpleTokenizer

  import context.dispatcher
  import scala.concurrent.Future
  import akka.pattern.pipe

  def receive = {
    case ReplyToStatus(status) => 
      val replyName = status.getUser.getScreenName
      val candidatesFuture = getReplies(status, 138-replyName.length)
      candidatesFuture.map { candidates =>
        candidates.toSet.headOption.map({ replyText:String => 
          val reply = "@" + replyName + " " + replyText
          log.info("Candidate reply: " + reply)
          new StatusUpdate(reply).inReplyToStatusId(status.getId)
        })
      } pipeTo sender
  }

  def getReplies(status: Status, maxLength: Int): Future[Seq[String]]

}

/**
 * An actor that constructs replies to a given status.
 */
class SynonymReplier extends BaseReplier {
  import Bot._ 
  import tshrdlu.util.English.synonymize
  import TwitterRegex._

  import context.dispatcher
  import scala.concurrent.Future

  def getReplies(status: Status, maxLength: Int = 140): Future[Seq[String]] = {
    log.info("Trying to reply synonym")
    val text = stripLeadMention(status.getText).toLowerCase
    val synTexts = (0 until 10).map(_ => Future(synonymize(text))) 
    Future.sequence(synTexts).map(_.filter(_.length <= maxLength))
  }

}

/**
 * An actor that constructs replies to a given status.
 * For best results, tweet at me something related to one of the 
 * topics from the "20 Newsgroups" data
 * e.g. Religion, baseball, atheism, windows, hockey, mideast, pc hardware
 */
class TopicModelReplier extends BaseReplier {
  import Bot._ 
  import TwitterRegex._
  import tshrdlu.util.SimpleTokenizer

  import context.dispatcher
  import akka.pattern.ask
  import akka.util._
  import scala.concurrent.duration._
  import scala.concurrent.Future
  implicit val timeout = Timeout(10 seconds)

  val modeler = new TopicModeler("minTopicKeys.txt")

  def getReplies(status: Status, maxLength: Int = 140): Future[Seq[String]] = {
    log.info("Trying to reply via topic models")
    val text = stripLeadMention(status.getText).toLowerCase
    val statusTopicList = SimpleTokenizer(text)
				.filter(_.length > 4)
				.toSet
				.take(3)
				.toList
				.flatMap(w => modeler.wordTopicsMap.get(w))
				.flatten

	val topicWords:List[String] = statusTopicList.map(topic => 
		modeler.topicWordsMap.getOrElse(topic,Set(" "))).take(4).flatten

	val statusQueryList :List[String] = topicWords
				.filter(_.length > 4)
                .filter(_.length < 11)
	        	.sortBy(- _.length)
				.distinct
    
    // Get a sequence of futures of status sequences (one seq for each query)
    val statusSeqFutures: Seq[Future[Seq[Status]]] = 
		if(statusQueryList.length <1) {
			SimpleTokenizer(text)
				.filter(_.length > 3)
				.filter(_.length < 10)
				.filterNot(_.contains('/'))
				.filter(tshrdlu.util.English.isSafe)
				.sortBy(- _.length)
				.take(3) 
				.map(w => (context.parent ? 
					SearchTwitter(new Query(w))).mapTo[Seq[Status]])}
		else { statusQueryList
    			.map(w => (context.parent ? 
					SearchTwitter(new Query(w))).mapTo[Seq[Status]])}

    // Convert this to a Future of a single sequence of candidate replies
    val statusesFuture: Future[Seq[Status]] =
      	Future.sequence(statusSeqFutures).map(_.flatten)

	statusesFuture.map{x => extractText(x, statusTopicList.toSet)}
  }

  /**
   * Go through the list of tweets, gets "proper" tweets, determines
   * topic distribution vectors of said tweets, calculates similarities
   * between original tweet and candidate tweets
   * Returns most similar tweeet
   */
  def extractText(statusList: Seq[Status], statusTopics: Set[String]) = {
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
    
    val topicSimilarity = topicDistributions.map(ids => 
		ids.toSet.intersect(statusTopics).size * {
			if(statusTopics.size -ids.toSet.size ==0 ) 1 
			else (1/math.abs(statusTopics.size - ids.toSet.size)).toDouble})
    
    val topTweet = topicSimilarity.toList.zip(useableTweets).maxBy(_._1)._2

    List(topTweet)
  }

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

/**
 * An actor that constructs replies to a given status.
 */
class StreamReplier extends BaseReplier {
  import Bot._
  import TwitterRegex._
  import tshrdlu.util.SimpleTokenizer

  import context.dispatcher
  import akka.pattern.ask
  import akka.util._
  import scala.concurrent.duration._
  import scala.concurrent.Future
  implicit val timeout = Timeout(10 seconds)

  /**
   * Produce a reply to a status.
   */
  def getReplies(status: Status, maxLength: Int = 140): Future[Seq[String]] = {
    log.info("Trying to reply stream")

    val text = stripLeadMention(status.getText).toLowerCase
    
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
      Future.sequence(statusSeqFutures).map(_.flatten)

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


/**
 * An actor that constructs replies to a given status based on synonyms.
 */
class SynonymStreamReplier extends StreamReplier {
  import Bot._
  import tshrdlu.util.SimpleTokenizer

  import context.dispatcher
  import akka.pattern.ask
  import akka.util._
  import scala.concurrent.duration._
  import scala.concurrent.Future

  import tshrdlu.util.English._
  import TwitterRegex._
  override implicit val timeout = Timeout(10000)


  override def getReplies(status: Status, maxLength: Int = 140): Future[Seq[String]] = {
    log.info("Trying to do synonym search")
    val text = stripLeadMention(status.getText).toLowerCase

    // Get two words from the tweet, and get up to 5 synonyms each (including the word itself).
    // Matched tweets must contain one synonym of each of the two words.

    val query:String = SimpleTokenizer(text)
      .filter(_.length > 3)
      .filter(_.length < 10)
      .filterNot(_.contains('/'))
      .filter(tshrdlu.util.English.isSafe)
      .filterNot(tshrdlu.util.English.stopwords(_))
      .take(2).toList
      .map(w => synonymize(w, 5))
      .map(x=>x.mkString(" OR ")).map(x=>"("+x+")").mkString(" AND ")

    log.info("searched for: " + query)

    val futureStatuses = (context.parent ? SearchTwitter(new Query(query))).mapTo[Seq[Status]]

    futureStatuses.map(_.flatMap(getText).filter(_.length <= maxLength))
 }

}


/**
 * An actor that constructs replies to a given status.
 */
class BigramReplier extends BaseReplier {
  import Bot._
  import TwitterRegex._
  import tshrdlu.util.SimpleTokenizer

  import context.dispatcher
  import akka.pattern.ask
  import akka.util._
  import scala.concurrent.duration._
  import scala.concurrent.Future
  implicit val timeout = Timeout(10 seconds)

  /**
   * Produce a reply to a status using bigrams
   */
  lazy val stopwords = tshrdlu.util.English.stopwords_bot
  def getReplies(status: Status, maxLength: Int = 140): Future[Seq[String]] = {
    log.info("Trying to reply stream")

    val text = stripLeadMention(status.getText).toLowerCase
    
    // Get a sequence of futures of status sequences (one seq for each query)

    val bigram = Tokenize(text)
      .sliding(2)
      .filterNot(z => (stopwords.contains(z(0))||stopwords.contains(z(1))))
      .flatMap{case Vector(x,y) => List(x+" "+y)}
      .toList
      .sortBy(-_.length)

    val statusSeqFutures: Seq[Future[Seq[String]]] = bigram
      .takeRight(5)
      .map(w => (context.parent ? SearchTwitter(new Query("\""+w+"\""))).mapTo[Seq[Status]].map(_.flatMap(getText).toSeq))
    
    //statusSeqFutures.foreach(println)
    // Convert this to a Future of a single sequence of candidate replies
    val statusesFuture: Future[Seq[String]] =
      extractText(statusSeqFutures,bigram.toList)

    //statusesFuture.foreach(println)
    // Filter statuses to their text and make sure they are short enough to use.
    statusesFuture.filter(_.length <= maxLength)
  }

  def extractText(statusList: Seq[Future[Seq[String]]],bigram:List[String]): Future[Seq[String]] = {
    val bigramMap = Future.sequence(statusList).map(_.flatten)
    //bigramMap.foreach(println)
    val sortedMap = bigramMap.map { tweet => {
      tweet.flatMap{ x => { 
        Tokenize(x)
          .sliding(2)
          .filterNot(z => (stopwords.contains(z(0))||stopwords.contains(z(1))))
          .map(bg => bg.mkString(" ") -> x) toMap
      }}.filter { case (p,q) => bigram.contains(p)}
    }}

    val bigramSeq = sortedMap.map(_.map(_._2))
    bigramSeq
  }

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

/**
 * An actor that constructs replies to a given status.
 */
class LuceneReplier extends BaseReplier {
  import Bot._
  import TwitterRegex._
  import tshrdlu.util.{English, Lucene, SimpleTokenizer}

  import context.dispatcher
  import akka.pattern.ask
  import akka.util._
  import scala.concurrent.duration._
  import scala.concurrent.Future

  def getReplies(status: Status, maxLength: Int = 140): Future[Seq[String]] = {
    log.info("Trying to do search replies by Lucene")
    val text = status.getText.toLowerCase
	  val StripLeadMentionRE(withoutMention) = text
	  val query = SimpleTokenizer(withoutMention)
	    .filter(_.length > 2)
	    .toList
	    .mkString(" ")
      val replyLucene = Lucene.read(query)
    Future(replyLucene).map(_.filter(_.length <= maxLength))
  }

}

/**
 * A replier that replies based on unsupervised noun phrase chunking of a given tweet.
 */
class ChunkReplier extends BaseReplier {
  import Bot._
  import tshrdlu.util.{English, Lucene, SimpleTokenizer}
  import jarvis.nlp.TrigramModel
  import jarvis.nlp.util._
  import scala.concurrent.Future  
  import TwitterRegex._
  import akka.pattern.ask
  import akka.util._
  import context.dispatcher
  import scala.concurrent.duration._
  import scala.concurrent.Future
  import java.net.URL

  implicit val timeout = Timeout(10 seconds)

  //A Trigram language model based on a dataset of mostly english tweets
  val LanguageModel = TrigramModel(SPLReader(this.getClass().getResource("/chunking/").getPath()))

  val Chunker = new Chunker()

  def getReplies(status: Status, maxLength: Int = 140): Future[Seq[String]] = {
    log.info("Getting chunk tweets")
    val text = status.getText.toLowerCase
    val StripLeadMentionRE(withoutMention) = text
    val selectedChunks = Chunker(withoutMention)
      .map(c => (LanguageModel(SimpleTokenizer(c)), c))
      .sorted
      .take(2)
      .map(_._2)
    
     val statusList: Seq[Future[Seq[Status]]] = selectedChunks
         .map(chunk => (context.parent ? SearchTwitter(new Query(chunk))).mapTo[Seq[Status]])

    val statusesFuture: Future[Seq[Status]] = Future.sequence(statusList).map(_.flatten)

    statusesFuture
      .map(status => extractText(status))
      .map(_.filter(_.length <= maxLength))
  }

  /**
   * Go through the list of Statuses, filter out the non-English ones,
   * strip mentions from the front, filter any that have remaining
   * mentions, and then return the head of the set, if it exists.
   */
   def extractText(statusList: Seq[Status]): Seq[String] = {
     val useableTweets = statusList
       .map(_.getText)
       .map {
         case StripMentionsRE(rest) => rest
         case x => x
       }.filter(tweet => tshrdlu.util.English.isEnglish(tweet) 
                       &&  tshrdlu.util.English.isSafe(tweet)
                       && !tweet.contains('@')
                       && !tweet.contains('/'))
      .map(t => (LanguageModel(SimpleTokenizer(t)), t))
      .sorted
      .reverse
      .map{ case (k,t) => t}
      //given the set of potential tweets, return the tweet that has
      //the highest probability according to the language model
      Seq(if (useableTweets.isEmpty) "I don't know what to say." else useableTweets.head)
  }
}

/**
 * An actor that responds to requests to make sandwiches.
 *
 * @see <a href="http://xkcd.com/149/">http://xkcd.com/149/</a>
 */
class SudoReplier extends BaseReplier {
  import scala.concurrent.Future
  import context.dispatcher

  lazy val MakeSandwichRE = """(?i)(?:.*(\bsudo\b))?.*\bmake (?:me )?an?\b.*\bsandwich\b.*""".r

  def getReplies(status: Status, maxLength: Int = 140): Future[Seq[String]] = {
    log.info("Checking for sandwich requests")
    val text = TwitterRegex.stripLeadMention(status.getText)
    val replies: Seq[String] = Seq(text) collect {
      case MakeSandwichRE(sudo) => {
        Option(sudo) match {
          case Some(_) => "Okay."
          case None => "What? Make it yourself."
        }
      }
    }
    Future(replies.filter(_.length <= maxLength))
  }
}

/** An actor that responds to a tweet if it can be replied 
* by "Thats what she said". This is based on the work done by 
*Chloe Kiddon and Yuriy Brun , University of Washington
*That's What She Said: Double Entendre Identification
* Dataset from Edwin Chen
*/

class TWSSReplier extends BaseReplier {
  import scala.concurrent.Future
  import context.dispatcher
  import de.bwaldvogel.liblinear._; 
  import scala.collection.mutable.ArrayBuffer
  import tshrdlu.util.{TWSSModel, English,SimpleTokenizer}


  val vocabulary = English.vocabularyTWSS.map(line => line.split(" ")(0)).toIndexedSeq
  val IDFMap:Map[String,Int] = English.vocabularyTWSS.map { line=>
    val tokens = line.split(" ");
    (tokens(0),tokens(1).toInt)
  }.toMap

  def getReplies(status: Status , maxLength:Int = 140): Future[Seq[String]] ={
    log.info("Checking if tweet can be responded with TWSS")
    val tweet = TwitterRegex.stripLeadMention(status.getText.toLowerCase)
    val tweetMap:Map[String,Int] = SimpleTokenizer(tweet)
    .groupBy(x=> x)
    .mapValues(x=> x.length)

    val twssModel = TWSSModel()
    val featureVector = getFeatureVector(tweetMap)
    val prob = Array(0.0,0.0);
    Linear.predictProbability(twssModel, getFeatureVector(tweetMap).toArray,prob);
   // println(prob.toList);
    val response = if(prob.toList(0) > 0.9 ) "Thats what she said !! :-P " else "Thats was exactly what I told him !! "
    Future(Seq(response));
  }
  def getFeatureVector(document:Map[String,Int]): ArrayBuffer[Feature] ={
    val feature = new ArrayBuffer[Feature](vocabulary.size);
    var index=1;

    vocabulary.foreach{ word=>
      
      if(document.contains(word))
      {
      val tf = document(word);
      val idf = Math.log(7887/IDFMap(word));
      val tf_idf = tf*idf;
      val featureNode:Feature = new FeatureNode(index,tf_idf);
      feature += featureNode
      }
      index +=1
    }
    feature
  }
}

class SentimentReplier extends BaseReplier {
  import Bot._
  import TwitterRegex._
  import tshrdlu.util.{Polarity, SimpleTokenizer}

  import context.dispatcher
  import scala.concurrent.duration._
  import scala.concurrent.Future
  import akka.pattern._
  import akka.util._

  lazy val polarity = new Polarity()
  implicit val timeout = Timeout(10 seconds)

  def getReplies(status: Status, maxLength: Int): Future[Seq[String]] = {
    val StripLeadMentionRE(withoutMention) = status.getText.toLowerCase
    val statusList =
      SimpleTokenizer(withoutMention)
      .filter(_.length > 3)
      .filter(_.length < 10)
      .filterNot(_.contains('/'))
      .filter(tshrdlu.util.English.isSafe)
      .sortBy(- _.length)
      .toList
      // Use a bigram instead of a unigram search
      .take(4)
      .sliding(2)
      .map(_.mkString(" "))
      .map(w => (context.parent ? SearchTwitter(new Query(w))).mapTo[Seq[Status]])

    val statusesFuture: Future[Seq[Status]] = Future.sequence(statusList).map(_.toSeq.flatten)

    statusesFuture
      .map(status => extractText(status, withoutMention))
      .map(_.filter(_.length <= maxLength))
  }

  def extractText(statusList: Seq[Status], tweet: String): Seq[String] = {
    val desiredSentiment = getSentiment(tweet)
    val mult = if(desiredSentiment == 0) -1 else 1
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

    Seq(if (useableTweets.isEmpty) "Sorry, but I can't help you with that." else useableTweets.sortBy(x => mult * Math.abs(getSentiment(x) + mult * desiredSentiment)).head)
  }

  def getSentiment(text: String) = {
    val words = SimpleTokenizer(text)
    val len = words.length.toDouble
    val percentPositive = words.count(polarity.posWords.contains) / len
    val percentNegative = words.count(polarity.negWords.contains) / len
    (percentPositive - percentNegative)
  }
}
