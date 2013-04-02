package tshrdlu.util

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

/**
 * A helper object for creating Scala Source instances given a
 * the location of a resource in the classpath, which includes
 * files in the src/main/resources directory.
 */
object Resource {
  import java.util.zip.GZIPInputStream


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
}

/**
 * A parent class for specific languages. Handles some common
 * functions.
 */
abstract class Language(code: String) {
  def stopwords: Set[String]
  def vocabulary: Set[String]

  lazy val resourceDir = "/lang/" + code
  def appendPath(subdir: String) = resourceDir + subdir
  def getLexicon(filename: String) = 
    Resource.asSource(appendPath("/lexicon/"+filename))
      .getLines
      .filterNot(_.startsWith(";")) // filter out comments
      .toSet

}

/**
 * English information.
 */
object English extends Language("eng") {
  lazy val stopwords = getLexicon("stopwords.english")
  lazy val vulgar = getLexicon("vulgar.txt.gz")
  lazy val stopwords_bot = getLexicon("stopwords.bot")
  lazy val vocabulary = getLexicon("masc_vocab.txt.gz") ++ stopwords

  def isEnglish(text: String) = {
    val words = SimpleTokenizer(removeNonLanguage(text).toLowerCase)
    val count = words.count(vocabulary) 
    count > 1 && count.toDouble/words.length > .3
  }

  def removeNonLanguage(text: String) =
    text.replaceAll("[@#][A-Za-z_]+","")
      .replaceAll("""http[^\s]+""","")
      .replaceAll("\\s+"," ")

  def isSafe(text: String) =  {
    val words = SimpleTokenizer(removeNonLanguage(text).toLowerCase)
    words.count(vulgar) == 0
  }

}
class English extends Language("eng") {
  lazy val stopwords = getLexicon("stopwords.english")
  lazy val vocabulary = getLexicon("masc_vocab.txt.gz") ++ stopwords
}
