package tshrdlu.project

import twitter4j._
import tshrdlu.twitter._
import tshrdlu.util.{English,SimpleTokenizer}
import io.Source

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
  val english = new English

  def isEnglish(text: String) = {
    val tweetWords = text.split("\\b").toList
                          .filter(word => !word.startsWith(List("@","#","http://")))
                          .map(word => word.toLowerCase())
                          .toSet

    tweetWords.intersect(english.vocabulary--english.stopwords).size > 0
  }
//    val tweetIterator = io.Source.stdin.getLines

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
object PolarityLocationStreamer extends FilteredStreamer with LocationFilter with PolarityStatusListener {
  override val outputInterval = 10
}


/**
 * For every tweet detected as English, compute its polarity based
 * on presence of positive and negative words, and output the label
 * with the tweet. Output statistics about polarity at specified
 * intervals (as given by the outputInterval variable).
 */
trait PolarityStatusListener extends EnglishStatusListener {
  import tshrdlu.util.DecimalToPercent
  import java.util.zip.GZIPInputStream
  import sys.process._

  val outputInterval = 100
  var numEnglishTweets = 0

  // Indices: 0 => +, 1 => -, 2 => ~
  val polaritySymbol = Array("+","-","~")
  var polarityCounts = Array(0.0,0.0,0.0)
  var placemarks = List[(String,String,String)]()

  def generateKMLFile(placemarks: List[(String,String,String)]) = {
    for (t <- placemarks) yield <Placemark>
      <name>{t._1}</name>
      <description>{t._2}</description>
      <Point>
        <coordinates>{t._3}</coordinates>
      </Point>
    </Placemark>
  }

  override def onStatus(status: Status) {
    val text = status.getText
    if (isEnglish(text) && status.getGeoLocation != null) {
      val polarityIndex = getPolarity(text)

      polarityCounts(polarityIndex) += 1
      numEnglishTweets += 1
      placemarks ::= (status.getUser.getScreenName,"Tweet",status.getGeoLocation.getLongitude+","+status.getGeoLocation.getLatitude+",0")

      println(polaritySymbol(polarityIndex) + ": " + text)

      if ((numEnglishTweets % outputInterval) == 0) {
	println("----------------------------------------")
	println("Number of English tweets processed: " + numEnglishTweets)
	println(polaritySymbol.mkString("\t"))
	println(polarityCounts.mkString("\t"))
	println(polarityCounts.map(_/numEnglishTweets).map(DecimalToPercent).mkString("\t"))
	println("----------------------------------------")

  val kmlXMLFile = <kml xmlns="http://www.opengis.net/kml/2.2">
    <Document>
      <name>egl33_phase2_tweets.kml</name>
    {generateKMLFile(placemarks)}
    </Document>
  </kml>

  scala.xml.XML.save("egl33_phase2_tweets.kml", kmlXMLFile)
  println("Google Earth KML file has been saved as egl33_phase2_tweets.kml")
  println("----------------------------------------")
      }
    }
  }

  /**
   * Read in a file as a Source, ensuring that the right thing
   * is done for gzipped files.
   */
  def asSource(location: String) = {
    val stream = this.getClass.getResourceAsStream(location)
    if (location.endsWith(".gz"))
      io.Source.fromInputStream(new GZIPInputStream(stream))
    else
      io.Source.fromInputStream(stream)
  }

  def positiveWords: Set[String] = getLexicon("positive-words.txt.gz")
  def negativeWords: Set[String] = getLexicon("negative-words.txt.gz")

  lazy val resourceDir = "/lang/eng"
  def appendPath(subdir: String) = resourceDir + subdir
  def getLexicon(filename: String) =
    asSource(appendPath("/lexicon/"+filename))
      .getLines
      .filterNot(_.startsWith(";")) // filter out comments
      .toSet


  /**
   * Given a text, return its polarity:
   *   0 for positive
   *   1 for negative
   *   2 for neutral
   *   For each tweet:
   *    count the number of positive words
   *    count the number of negative words
   *    if the number of positive words is greater than the number of negative words, the tweet is positive
   *    if the number of positive words is less than the number of negative words, the tweet is negative
   *    otherwise, the tweet is neutral
   */
  val random = new scala.util.Random
  def getPolarity(text: String) = {
    val tweetWords = text.split("\\b").toList
      .filter(word => !word.startsWith(List("@","#","http://")))
      .map(word => word.toLowerCase())
      .toSet

    val totalPos = tweetWords.intersect(positiveWords).size
    val totalNeg = tweetWords.intersect(negativeWords).size
    if (totalPos > totalNeg) 0 else if (totalPos < totalNeg) 1 else 2
  }

}
