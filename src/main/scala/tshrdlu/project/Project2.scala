package tshrdlu.project

import twitter4j._
import tshrdlu.twitter._
import tshrdlu.util.{English,SimpleTokenizer}
import tshrdlu.util.Polarity

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

val FilterRE = """(a|(?:RT)|[@#].+|.*[",\.\-':;!\?\d\\/<>\(\)\[\]\*$%&=\+#@]+.*)""".r

val lower = "abcdefghijklmnopqrstuvwxyz"
val upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
// val little2big = lower.zip(upper).toMap
val big2little = upper.zip(lower).toMap

// def mayCap (letter: Char): Char = {
//   if (lower.contains(letter)) little2big(letter)
//   else letter
// }

def mayLow (letter: Char): Char = {
  if (upper.contains(letter)) big2little(letter)
  else letter
}

def containsWord (word: String): Boolean = {

// val cap1stLetter = mayCap(word.head) + word.tail.map(x=>mayLow(x)).mkString("")
// val allCaps = word.map(x => mayCap(x)).mkString("")
val allLows = word.map(x => mayLow(x)).mkString("")

if (English.vocabulary(allLows)) true
else false

// if (English.vocabulary(word)|English.vocabulary(cap1stLetter)|
// English.vocabulary(allCaps)|English.vocabulary(allLows)) true
// else false

}


def getRealWords(text: String): IndexedSeq[String] = {
  SimpleTokenizer(text)
    .flatMap( x => x match { 
      case FilterRE(x) => None
      case _ => Some(x)
      })
  }

def isEnglish(text: String) = {
  val words = getRealWords(text)
  val total = if (words.length == 0) 1
            else words.length
  val numEng = (for (word <- words) yield containsWord(word))
  .filter(x=>x==true).length
  val prop = (numEng.toDouble)/total

  if (prop >= 0.40) true
  else false

}

// def isEnglishX (text: String) = {

// val wordResults = SimpleTokenizer(text)
//     .map(x=>x match { 
//       case FilterRE(x) => (x, "JUNK")
//       case _ => (x, containsWord(x))
//       })

// val numAll = wordResults.filter(x=>x._2 !="JUNK").length
// val numEng = wordResults.filter(x=>x._2 !="JUNK").filter(x=>containsWord(x._1)==true).length
// val prop = numEng.toDouble/(numAll+.00001)

// (wordResults, numEng, numAll, prop)

// }


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
    val prefix = if (isEnglish(text)) "[ENGLISH]" else "[OTHER]"
    println(prefix + " " + text)

  }

  // val testTweet = "Que intenso!"
  // println(isEnglishX(testTweet))
  // println("VERDICT :" + isEnglish(testTweet))

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

  val outputInterval = 100

  var numEnglishTweets = 0

  // Indices: 0 => +, 1 => -, 2 => ~
  val polaritySymbol = Array("+","-","~")
  var polarityCounts = Array(0.0,0.0,0.0)

  override def onStatus(status: Status) {
    val text = status.getText
    if (isEnglish(text)==true) {
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

  def mayLowWord(word:String) = {
    word.map(x=>mayLow(x)).mkString("")
  }

  def getPolarity(text: String) = {


    val pos = getRealWords(text).filter(word=>
      Polarity.posWords(mayLowWord(word))).length
    val neg = getRealWords(text).filter(word=>
      Polarity.negWords(mayLowWord(word))).length
    if (pos > neg) 0
    else if (pos < neg) 1
    else 2
  }

}

object Polarity extends Polarity





  // val TheRE = """(?i)\bthe\b""".r // Throw me away!
  //   // Remove this and do better.
  //   !TheRE.findFirstIn(text).isEmpty
