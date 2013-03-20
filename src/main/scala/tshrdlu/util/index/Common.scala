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
import java.io.{ByteArrayInputStream,ByteArrayOutputStream}
import java.io.{ObjectInputStream,ObjectOutputStream}

import org.apache.commons.codec.binary.Base64
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.en.EnglishAnalyzer
import org.apache.lucene.document.{Document,StoredField}
import org.apache.lucene.store.{Directory,SimpleFSDirectory}
import org.apache.lucene.util.Version


/**
 * Provides common functionality for reader/writer factories.
 *
 * @tparam T the type of object in the index (must be serializable)
 */
abstract class ReaderOrWriterFactory[T] {

  /**
   * Finds and uses an index by name. See
   * [[tshrdlu.util.index.Settings.BaseIndexPath]] for a decription of where
   * the index is located on disk.
   *
   * @param indexName the name of the index
   */
  def apply(indexName: String): T = {
    apply(new File(Settings.BaseIndexPath + "/index-" + indexName))
  }

  /**
   * Uses an index in the specified directory.
   *
   * @param indexDirectory the path to the index
   */
  def apply(indexDirectory: File): T = {
    val index = new SimpleFSDirectory(indexDirectory)
    apply(index)
  }

  /**
   * Uses an existing object representing a Lucene index.
   *
   * @param index the index
   */
  def apply(index: Directory): T
}


/**
 * Converts between Lucene <code>Document</code>s and objects to index.
 * Objects are serialized and written to a field in the <code>Document</code>.
 * If the concrete implementation of this class provides a unique ID for each
 * object, the ID is also written to a field.
 *
 * @param idFieldName         the field name to use for the ID
 * @param serializedFieldName the field name to use for the serialized object
 */
abstract class ObjectToDocument[T](
    val idFieldName: String = "_object_id_",
    val serializedFieldName: String = "_base64_object_")
extends (T => Document) {

  /**
   * Convert an object of type <code>T</code> to a Lucene <code>Document</code>.
   *
   * @param theObject the object to convert to a <code>Document</code>
   */
  def apply(theObject: T): Document = {
    val document = new Document()
    getId(theObject) collect {
      case id => document.add(new StoredField(idFieldName, id))
    }
    document.add(new StoredField(serializedFieldName, serialize(theObject)))

    addFields(document, theObject)
    document
  }

  /**
   * Converts a Lucene <code>Document</code> to an object of type
   * <code>T</code>.
   *
   * @param document the <code>Document</code> to convert to a object
   */
  def unapply(document: Document): Option[T] = {
    try {
      Some(deserialize(document.get(serializedFieldName)))
    } catch {
      case e: Exception => None
    }
  }

  /**
   * When converting to a <code>Document</code>, adds fields to the index that
   * can be used from search queries.
   *
   * @param document  the document to add fields to
   * @param theObject the object used to populate the field values
   */
  def addFields(document: Document, theObject: T)

  /**
   * Gets a unique ID for the object if desired, otherwise <code>None</code>.
   * If an ID is returned, it is added to the index.
   *
   * @param theObject the object to extract an ID from
   * @return          <code>Some(id)</code> or <code>None</code>
   */
  def getId(theObject: T): Option[Long]

  private def serialize(theObject: T): String = {
    val baos = new ByteArrayOutputStream()
    val oas = new ObjectOutputStream(baos)
    oas.writeObject(theObject)
    Base64.encodeBase64String(baos.toByteArray())
  }

  private def deserialize(serializedObject: String): T = {
    val b = Base64.decodeBase64(serializedObject)
    val bi = new ByteArrayInputStream(b)
    val si = new ObjectInputStream(bi)
    si.readObject().asInstanceOf[T]
  }
}


/**
 * Creates a standard English analyzer for tokenizing text.
 */
object EnglishAnalyzerCreator extends (Version => Analyzer) {
  def apply(luceneVersion: Version): Analyzer = new EnglishAnalyzer(luceneVersion)
}


/**
 * Miscellaneous settings.
 */
object Settings {

  /**
   * The version of the Lucene index format to use.
   */
  val LuceneVersion = Version.LUCENE_42

  /**
   * The directory where an index is stored if it is referenced by name. This
   * value is set to "<code>[base]/index-[indexName]/</code>" where:
   *
   * <ul>
   *   <li><code>[base]</code> is the value of the
   *       <code>TSHRDLU_INDEX_DIR</code> environment variable if set,
   *       otherwise the temp directory, in the dir "tshrdlu".
   *   <li><code>[indexName]</code> is the name of the index</li>
   * </ul>
   */
  val BaseIndexPath: String = {
    val result = Option(System.getenv("TSHRDLU_INDEX_DIR")).getOrElse(
      new File(new File(System.getProperty("java.io.tmpdir")), "tshrdlu")).toString
    new File(result).mkdirs()
    result
  }
}
