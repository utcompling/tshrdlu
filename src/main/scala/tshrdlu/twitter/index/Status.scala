package tshrdlu.twitter.index

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

import org.apache.lucene.document.{Document,Field,StringField,TextField}
import twitter4j.Status

import tshrdlu.util.index._


/**
 * Prepares <code>Status</code> objects to be added to a Lucene index.
 */
object StatusToDocument extends ObjectToDocument[Status] {

  /**
   * Adds fields to the Lucene <code>Document</code> so they can be searched.
   *
   * @param document the document to add fields to
   * @param status   the status to extract field values from
   */
  def addFields(document: Document, status: Status) {
    document.add(new TextField("text", status.getText, Field.Store.NO))

    val userScreenName = {
      val user = status.getUser
      if (user == null)
        ""
      else
        user.getScreenName
    }
    document.add(new StringField("user_screen_name", userScreenName, Field.Store.NO))
  }

  /**
   * Returns the Twitter status ID as the ID to store in the index.
   */
  def getId(status: Status): Option[Long] = Some(status.getId)
}


/**
 * Creates objects to read statuses from an index.
 */
object StatusReaderFactory extends ObjectReaderFactory[Status](StatusToDocument)


/**
 * Creates objects to write statuses to an index.
 */
object StatusWriterFactory extends ObjectWriterFactory[Status](StatusToDocument)
