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

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.index.{IndexWriter,IndexWriterConfig}
import org.apache.lucene.store.{Directory,RAMDirectory}
import org.apache.lucene.util.Version


/**
 * Provides write access to a Lucene index of objects. The <code>close</code>
 * method should be called once all objects have been indexed.
 *
 * @constructor             create a new object writer
 * @tparam T                the type of object in the index (must be
 *                          serializable)
 * @param index             the Lucene index to write to
 * @param indexWriterConfig the configuration for writing to the index
 * @param objectToDocument  converts objects of type <code>T</code> to Lucene
 *                          <code>Document</code>s
 */
class ObjectWriter[T](
    val index: Directory,
    indexWriterConfig: IndexWriterConfig,
    objectToDocument: ObjectToDocument[T]) {
  protected val indexWriter = new IndexWriter(index, indexWriterConfig)

  // Ensure the index is created so readers can open it even if it is empty
  commit()

  /**
   * Adds an object to the index.
   *
   * @param theObject     the object to add to the index
   * @param commitChanges if true, changes to the index are committed after
   *                      adding the object. This hurts performance but gives
   *                      index readers earlier access to the object.
   */
  def add(theObject: T, commitChanges: Boolean = true) {
    indexWriter.addDocument(objectToDocument(theObject))
    if (commitChanges) {
      commit()
    }
  }

  /**
   * Commits changes to the index and closes the underlying
   * <code>IndexWriter</code> to release the write lock.
   */
  def close() {
    indexWriter.close()
  }

  /**
   * Commits changes to the index.
   */
  def commit() {
    indexWriter.commit()
  }
}


/**
 * Constructs [[ObjectWriter]]s to write objects to a Lucene index.
 *
 * @tparam T               the type of object in the index (must be
 *                         serializable)
 * @param objectToDocument converts objects of type <code>T</code> to Lucene
 *                         <code>Document</code>s
 * @param analyzerCreator  creates the Lucene <code>Analyzer</code> for
 *                         extracting terms from text. This should create an
 *                         analyzer with the same behavior as the one used
 *                         to read from the index.
 * @param luceneVersion    the version of the Lucene index
 * @param openMode         the mode to use when opening the index
 */
class ObjectWriterFactory[T](
    objectToDocument: ObjectToDocument[T],
    analyzerCreator: (Version => Analyzer) = EnglishAnalyzerCreator,
    luceneVersion: Version = Settings.LuceneVersion,
    openMode: IndexWriterConfig.OpenMode = IndexWriterConfig.OpenMode.CREATE_OR_APPEND)
extends ReaderOrWriterFactory[ObjectWriter[T]] {

  def apply(): ObjectWriter[T] = {
    val index = new RAMDirectory()
    apply(index)
  }

  def apply(index: Directory): ObjectWriter[T] = {
    val analyzer = analyzerCreator(luceneVersion)
    val config = new IndexWriterConfig(luceneVersion, analyzer)
    config.setOpenMode(openMode)
    new ObjectWriter[T](index, config, objectToDocument)
  }
}
