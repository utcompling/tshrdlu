package tshrdlu.util.index

/**
 * Copyright 2013 Nick Wilson
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

import java.io.File

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.{IndexSearcher,TopScoreDocCollector}
import org.apache.lucene.store.{Directory,RAMDirectory}
import org.apache.lucene.util.Version


/**
 * Provides read access to a Lucene index of objects.
 *
 * @constructor            create a new object reader
 * @tparam T               the type of object in the index (must be serializable)
 * @param index            the Lucene index to read from
 * @param queryParser      parses a string containing a search query
 * @param objectToDocument converts Lucene <code>Document</code>s to objects
 *                         of type <code>T</code>
 */
class ObjectReader[T](
    index: Directory,
    queryParser: QueryParser,
    objectToDocument: ObjectToDocument[T]) {
  protected var indexReader = DirectoryReader.open(index)
  protected var searcher = new IndexSearcher(indexReader)

  /**
   * Searches for objects in the index matching a query. The matching
   * objects are returned in an iterator along with their score used
   * for ranking the results.
   *
   * Prior to doing the search, the indexed is reopened if necessary to get
   * access to documents that may have recently been added to the index.
   *
   * @param query   the search query
   * @param maxHits the maximum number of results to return
   * @return        the results in descending order of score
   * @see <a href="http://lucene.apache.org/core/4_2_0/queryparser/org/apache/lucene/queryparser/classic/package-summary.html">Lucene query parser format</a>
   */
  def search(query: String, maxHits: Int = 10): Iterator[(T, Float)] = {
    reopenIfChanged()

    // Do the search
    val queryObj = queryParser.parse(query)
    val collector = TopScoreDocCollector.create(maxHits, true)
    searcher.search(queryObj, collector)
    val hits = collector.topDocs().scoreDocs

    // Convert the Lucene documents into objects of the appropriate type
    // and return them with their scores
    hits.map { hit =>
      val document = searcher.doc(hit.doc)
      val objectToDocument(theObject) = document
      (theObject, hit.score)
    }.toIterator
  }

  /**
   * Reopens the index if it has changed.
   */
  protected def reopenIfChanged() {
    val newReader = DirectoryReader.openIfChanged(indexReader)
    if (newReader != null) {
      indexReader = newReader
      searcher = new IndexSearcher(indexReader)
    }
  }
}


/**
 * Constructs [[ObjectReader]]s to read objects from a Lucene index.
 *
 * @tparam T                 the type of object in the index (must be
 *                           serializable)
 * @param objectToDocument   converts Lucene <code>Document</code>s to objects
 *                           of type <code>T</code>
 * @param defaultSearchField the default field to search when doing a query.
 *                           For example, a query "title:blah" will search the
 *                           "title" field while a query "blah" will search
 *                           the field specified by this parameter.
 * @param analyzerCreator    creates the Lucene <code>Analyzer</code> for
 *                           extracting terms from text. This should create an
 *                           analyzer with the same behavior as the one used
 *                           to write to the index.
 * @param luceneVersion      the version of the Lucene index
 */
class ObjectReaderFactory[T](
    objectToDocument: ObjectToDocument[T],
    defaultSearchField: String = "text",
    analyzerCreator: (Version => Analyzer) = EnglishAnalyzerCreator,
    luceneVersion: Version = Settings.LuceneVersion)
extends ReaderOrWriterFactory[ObjectReader[T]] {

  def apply(index: Directory): ObjectReader[T] = {
    val analyzer = analyzerCreator(luceneVersion)
    val queryParser = new QueryParser(luceneVersion, defaultSearchField, analyzer)
    new ObjectReader[T](index, queryParser, objectToDocument)
  }
}


/**
 * Provides a main method to perform searches on indexes.
 *
 * @param readerFactory the factory used to create an [[ObjectReader]]
 */
abstract class ObjectIndexSearcher[T](readerFactory: ObjectReaderFactory[T]) {

  /**
   * Searches an index and prints the top hits.
   *
   * @param args the first argument is a name of an index (see
   *             [[tshrdlu.util.index.Settings.BaseIndexPath]] for a
   *             decription of where the index is located on disk) or a full
   *             path to an index. The second argument is the search query.
   */
  def main(args: Array[String]) {
    val Array(indexNameOrPath, query) = args

    println("Searching index '" + indexNameOrPath + "': " + query + "\n")

    // Treat the first argument as a full path to an index if the directory
    // exists, otherwise treat it as an index name
    val path = new File(indexNameOrPath)
    val reader = if (path.exists) {
      readerFactory(path)
    } else {
      readerFactory(indexNameOrPath)
    }

    // Do the search and print the top hits
    reader.search(query).foreach {
      case (theObject, score) => {
        println(resultToString(theObject, score))
      }
    }
  }

  /**
   * Returns a string representing a single hit.
   *
   * @param theObject the object retrieved from the index
   * @param score     the score used to rank the hit
   */
  def resultToString(theObject: T, score: Float): String
}
