package tshrdlu.twitter

import java.io._
import upparse.cli.Main
import upparse.corpus.{StopSegmentCorpus, BasicCorpus, CorpusUtil}
import tshrdlu.util.Resource
import java.net.URL

/*
@author Stephen Pryor (spryor)
*/

class Chunker {

  //given a tweet, return the chunks
  def apply(tweet: String) = extractChunks(chunkTweet(tweet))

  lazy val resourceDirectory = this.getClass().getResource("/chunking/").getPath()

  //the file with a tweet to chunk
  lazy private val tweetFile = resourceDirectory+"evalTweet.txt" 
 
  //the arguments to initialize upparse
  lazy private val upparseArgs: Array[String] = Array("chunk",
                                        "-chunkerType", "PRLG",
                                        "-chunkingStrategy", "UNIFORM",
                                        "-encoderType", "BIO",
                                        "-emdelta", ".0001",
                                        "-smooth", ".1",
                                        "-train", resourceDirectory+"trainingTweets.txt",
                                        "-test", tweetFile,
                                        "-trainFileType", "SPL",
                                        "-testFileType", "SPL")
  
  lazy val Chunks = """[(]([^()]+)[)]""".r  

  //create a upparse object
  lazy val upparser = new Main(upparseArgs)
  
  //create and train the upparse model 
  lazy val model = {
    val m = upparser.chunk_special()
    while(m.anotherIteration()) {
      m.updateWithEM(upparser.outputManager.getStatusStream())
    }
    m
  } 

  //A function to create and write to files
  def printToFile(f: java.io.File)(op: java.io.PrintWriter => Unit) {
    val p = new java.io.PrintWriter(f)
    try { op(p) } finally { p.close() }
  }

  //A simple function for writing strings to a file
  def writeTextToFile(filename: String, text: String) = {
    printToFile(new File(filename))(p => {
      p.println(text)
    })
  } 

  /**
   * chunkTweet takes a new tweet and runs upparse on it to 
   * chunk the raw tweet.
   */
  def chunkTweet(tweet: String) = {
    writeTextToFile(tweetFile, tweet)
    val newTweet = CorpusUtil.stopSegmentCorpus(upparser.evalManager.alpha,
      Array(tweetFile),
      upparser.evalManager.testFileType,
      upparser.evalManager.numSent,
      upparser.evalManager.filterLength,
      upparser.evalManager.noSeg,
      upparser.evalManager.reverse)
    val chunkerOutput = model.getCurrentChunker().getChunkedCorpus(newTweet)
    chunkerOutput.clumps2str(chunkerOutput.getArrays()(0))
  }

  /**
   * extractChunks returns an indexed sequence of strings where 
   * each string is chunked sequence of words found by upparse.
   */
  def extractChunks(chunkedTweet: String) = {
    (Chunks findAllIn chunkedTweet).map(_.replaceAll("[^a-zA-Z\\s]+","")).toIndexedSeq
  }
}
