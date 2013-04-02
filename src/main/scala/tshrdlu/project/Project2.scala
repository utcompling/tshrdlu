package tshrdlu.project

import twitter4j._
import tshrdlu.twitter._
import tshrdlu.util.{English,SimpleTokenizer}

/**
 * Show only tweets that appear to be English.
 */
object EnglishStatusStreamer extends BaseStreamer with EnglishStatusListener

/**
 * Debug the English detector.
 */
object EnglishStatusStreamerDebug extends BaseStreamer with EnglishStatusListenerDebug

/**
 * Output only tweets detected as English.
 */
trait EnglishStatusListener extends StatusListenerAdaptor {

  /**
   * If a status' text is English, print it.
   */
  override def onStatus(status: Status) { 
    val text = status.getText
    if (isEnglish(text)) 
      println(text)
  }

  /**
   * Test whether a given text is written in English.
   */
 // val TheRE = """(?i)\bthe\b""".r // Throw me away!
 val start = """(?:[#@])|\b(?:http)"""
 val dictionary = new English
  def isEnglish(text: String) = {
    // Remove this and do better.
    //!TheRE.findFirstIn(text).isEmpty
    val seqText =  SimpleTokenizer(text).distinct
    val bool = seqText.filter(x => !x.startsWith(start)).map(x => dictionary.vocabulary.contains(x))
    val trueCount = bool.count(_==true)
    val falseCount = bool.count(_==false)
    if(trueCount>falseCount)
      true
    else
      false
    /*var counter =0
    var i =0
    var bool = false
    while(counter<=round(0.5*seqText.size&& i<seqText.size)){
      bool = English.vocabulary.contains(seqText(i))

      counter= counter+1
    }*/

  }

}

/**
 * Output both English and non-English tweets in order to improve
 * the isEnglish method in EnglishStatusListener.
 */
trait EnglishStatusListenerDebug extends EnglishStatusListener {

  /**
   * For each status, print it's text, prefixed with the label
   * [ENGLISH] for those detected as English and [OTHER] for
   * those that are not.
   */
  override def onStatus(status: Status) { 
    val text = status.getText
    val prefix = if (isEnglish(text)) "[ENGLISH]" else "[OTHER]  "
    println(prefix + " " + text)
  }
}


/**
 * Output polarity labels for every English tweet and output polarity
 * statistics at default interval (every 100 tweets). Done for 
 * tweets from the Twitter sample.
 */
object PolarityStatusStreamer extends BaseStreamer with PolarityStatusListener

/**
 * Output polarity labels for every English tweet and output polarity
 * statistics at default interval (every 100 tweets). Filtered by provided
 * query terms.
 */
object PolarityTermStreamer extends FilteredStreamer with TermFilter with PolarityStatusListener

/**
 * Output polarity labels for every English tweet and output polarity
 * statistics at an interval of every ten tweets. Filtered by provided
 * query locations.
 */
object PolarityLocationStreamer extends FilteredStreamer with LocationFilter with PolarityStatusListener{
  override val outputInterval=10
}


/**
 * For every tweet detected as English, compute its polarity based
 * on presence of positive and negative words, and output the label
 * with the tweet. Output statistics about polarity at specified
 * intervals (as given by the outputInterval variable).
 */
trait PolarityStatusListener extends EnglishStatusListener {

  import tshrdlu.util.DecimalToPercent

  val outputInterval = 100

  var numEnglishTweets = 0

  // Indices: 0 => +, 1 => -, 2 => ~
  val polaritySymbol = Array("+","-","~")
  var polarityCounts = Array(0.0,0.0,0.0)

  override def onStatus(status: Status) {
    val text = status.getText
    if (isEnglish(text)) {
      val polarityIndex = getPolarity(text)
      polarityCounts(polarityIndex) += 1

      numEnglishTweets += 1

      println(polaritySymbol(polarityIndex) + ": " + text)

      if ((numEnglishTweets % outputInterval) == 0) {
	println("----------------------------------------")
	println("Number of English tweets processed: " + numEnglishTweets)
	println(polaritySymbol.mkString("\t"))
	println(polarityCounts.mkString("\t"))
	println(polarityCounts.map(_/numEnglishTweets).map(DecimalToPercent).mkString("\t"))
	println("----------------------------------------")
      }
    }
    
  }

  /**
   * Given a text, return its polarity:
   *   0 for positive
   *   1 for negative
   *   2 for neutral
   */
  val sentiment = new English
  val starts = """(?:[#@])|\b(?:http)"""
  //val positiveWords = sentiment.positiveWords
  //val random = new scala.util.Random
  lazy val positiveWords = sentiment.getLexicon("positive_words.txt.gz");
  lazy val negativeWords = sentiment.getLexicon("negative_words.txt.gz");
  def getPolarity(text: String) = {
    //random.nextInt(3)
    val seqText =  SimpleTokenizer(text).distinct
    if (seqText.contains(":)")||seqText.contains(":-)")) 0 else if(seqText.contains(":(")||seqText.contains(":-(")) 1 else{
    val sentimentMap = seqText.filter(x => !x.startsWith(starts)).map(x => if(positiveWords.contains(x)) "positive" else if(negativeWords.contains(x)) "negative" else "neutral")
    val positiveCount = sentimentMap.count(_=="positive")
    val negativeCount = sentimentMap.count(_=="negative")
    val neutralCount = sentimentMap.count(_=="neutral")
    if(positiveCount>  negativeCount)
      0
    else if(negativeCount > positiveCount)
      1
      else
      2
    }
  }

}
