//Author: Stephen Pryor
//Feb 27, 2013

package jarvis.nlp.util

import java.io.File

/* -------------------- Tokenizer - START -------------------- */

trait Tokenizer {
  def apply(text: String): IndexedSeq[String] 
}

/**
 * The SimpleTokenizer object taks a string and tokenizes words only
 * while removing puncuation except for ' which occur in words.
 */
object SimpleTokenizer extends Tokenizer {
  lazy val tokenPattern = """([<]?[a-zA-Z]+(['][a-zA-Z]+)?[>]?)""".r
    
  override def apply(text: String): IndexedSeq[String] = {
    (tokenPattern findAllIn text).toIndexedSeq
  }
}

/* -------------------- Tokenizer - END -------------------- */

/* -------------------- FileParser - START -------------------- */

/**
 * The FileParser trait is used take a file from a corpus and parse
 * the file to extract sentences as tokenized sequences of strings.
 */
trait FileParser {
  def parse(text: String, tokenize: Tokenizer): IndexedSeq[IndexedSeq[String]]
}

/**
 * The IdentityParser simply takes the contents of a file and tokenizes them.
 * That is, the entire file is handled as a single sentence (useful if you are
 * interested in extracting document level features).
 */
trait IdentityParser extends FileParser {
  override def parse(text: String, tokenize: Tokenizer): IndexedSeq[IndexedSeq[String]] =
    IndexedSeq(tokenize(text))
}

/**
 * The SPLParser assumes there is a sentence per line and tokenizes that sentence.
 */
trait SPLParser extends FileParser {
  override def parse(text: String, tokenize: Tokenizer): IndexedSeq[IndexedSeq[String]] =
    text.split("\n")
      .map(tokenize(_))
      .toIndexedSeq
}

/**
 * The PENNParser is used to parse the contents of Penn Treebank 
 * WSJ or BROWN .pos files. 
 */
trait PENNParser extends FileParser {
  val pattern = """([^\s]+/[^\s]+)""".r
  
  override def parse(text: String, tokenize: Tokenizer): IndexedSeq[IndexedSeq[String]] = text
            .replaceAll("[*x]{2,}.*[*x]", "") //Remove any comments
            .replaceAll("(=+)(\\s*=+)*", "$1") //Remove sequences of '=' 
            .trim
            .split("=+") //Split across '=' boundaries
            .filter(_.length > 0)
            .flatMap(x => {
              (pattern findAllIn x.trim) //Extract all word/POS pairs
              .mkString(" ")
              .split("/\\.") //Split across sentences
              .map(_.replaceAll("""/[^\s]+|[\\]""", "") //Remove all POS tags and \'s
                    .replaceAll("\\s+([n]?'[^\\s]+)|([;:])\\s*[;:]", "$1") //Remove sequences of ;'s or :'s and spaces that occur before words that start with ' or "n'"
                    .replaceAll("[\\d]+", "<num>") //replace numbers with the "<num>" place holder, not perfect but will function
                    .trim)
            }).filter(_.length > 1)
            .map(tokenize(_)) //Tokenize every sentence
            .toIndexedSeq
}

/* -------------------- FileParser - END -------------------- */

/* -------------------- CorpusReader - START -------------------- */
/**
 * The CorpusReader class is used to collect, read, and parse
 * all of the files in a directory ending with '.pos' or as specified.
 */
abstract class CorpusReader(fileEnding: String) extends FileParser {
  def apply(directory:String, tokenizer: Tokenizer = SimpleTokenizer) = {
    val dataset = new File(directory)
    if(!dataset.isDirectory) {
      println("Error[CorpusReader]: "+directory+" is not a directory.")
      System.exit(0)
    }
    grabFiles(dataset).flatMap(file => {
      parse(readPOSFile(file), tokenizer)
    }).filter(_.length > 1)
  }
  
  private[this] def readPOSFile(path:File) = {
    io.Source.fromFile(path).getLines.mkString("\n")
  }
  
  private[this] def grabFiles(f: File): IndexedSeq[File] = {
    val files = f.listFiles
    files.filter(file => !file.isDirectory && file.toString.endsWith(fileEnding)) ++ files.filter(_.isDirectory).flatMap(grabFiles)
  }
}

/* -------------------- CorpusReader - END -------------------- */

//Creates an object to read .pos files from the Penn treebank
object PENNReader extends CorpusReader(".pos") with PENNParser

//Create and object to read and parse files with a single
//sentence per file
object SPLReader extends CorpusReader(".txt") with SPLParser

