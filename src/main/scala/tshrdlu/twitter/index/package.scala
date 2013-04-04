package tshrdlu.twitter

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

/**
 * Provides support for writing objects to a Lucene index and searching them.
 * An index can be created in RAM, which is lost when the process exits, or can
 * be created on disk and used from multiple processes.
 *
 * ==Index Location==
 *
 * Creating an index in RAM is less likely to cause headaches as long as you do
 * not need to keep the data after the process exits. An on-disk index needs to
 * be maintained (e.g., the objects in it may need to be updated/reindexed if
 * the format of the index changes in future versions) and should be backed up.
 *
 * An index on disk is stored in a directory in the filesystem. This package
 * provides two ways to reference such an index:
 *
 *  1. '''Full Path''' - The full path to the directory containing the index
 *     such as `/tmp/indexes/index-status`
 *
 *  1. '''Name''' - The name of the index such as "`status`". In this case, the
 *  full path to the index will be `[base]/index-[indexName]/` where:
 *
 *    - `[base]` is the value of the
 *      `TSHRDLU_INDEX_DIR` environment variable if set,
 *      otherwise the folder "tshrdlu" in your temporary directory.
 *
 *    - `[indexName]` is the name of the index
 *
 * An on-disk index can be cleared by simple deleting the index directory.
 *
 * ==Indexing from the Command Line==
 *
 * Some apps (classes/objects with `main` methods) are provided to make it easy
 * to index status messages from Twitter's sample stream. Apps prefixed with
 * `SafeEnglish` automatically filter out non-English and unsafe (e.g.,
 * profane) tweets. The following command will collect status messages in an
 * index on disk named `my-status-index`. The index will be created if it does
 * not already exist, otherwise it will be appended to. The status messages are
 * also printed to the screen.
 *
 * {{{
 * $ tshrdlu run tshrdlu.twitter.index.SafeEnglishStatusIndexer my-status-index
 * }}}
 *
 * Status messages will continue to be indexed until the process is killed.
 *
 * Indexes on disk can be searched with the [[StatusSearcher]] app. For
 * example, to search the index we just created and display up to 10 tweets:
 *
 * {{{
 * $ tshrdlu run tshrdlu.twitter.index.StatusSearcher my-status-index "good morning"
 * Searching index 'my-status-index': good morning
 *
 * 4.3832407: Good morning
 * 2.1916203: Somebody tell me good morning :) don't everybody say it all at once !!!!
 * [...]
 * }}}
 *
 * The number preceeding each tweet is the score assigned to the tweet by
 * Lucene for ranking the results.
 *
 * Note that you can write an index from one process and read from another.
 * You could run [[SafeEnglishStatusIndexer]] to index tweets in one terminal
 * window and [[StatusSearcher]] in another to search the latest tweets.
 *
 * Additional apps are provided to index tweets filtered by user ID, terms, or
 * location:
 *   - [[SafeEnglishStatusIdIndexer]]
 *   - [[SafeEnglishStatusTermIndexer]]
 *   - [[SafeEnglishStatusLocationIndexer]]
 *
 * For example, to index random tweets containing "cat", "dog", or "pig":
 *
 * {{{
 * $ tshrdlu run tshrdlu.twitter.index.SafeEnglishStatusTermIndexer animal-statuses cat dog pig
 * }}}
 *
 * ==Indexing from Code==
 *
 * Working with a status index from code is easy using [[StatusWriterFactory]]
 * and [[StatusReaderFactory]].
 *
 * {{{
 * // Get index writer and reader
 * val indexName = "my-status-index"
 * val writer = StatusWriterFactory(indexName)
 * val reader = StatusReaderFactory(indexName)
 *
 * // Add a tweet (assuming `newStatus` is a Twitter4J `Status` object)
 * writer.add(newStatus)
 *
 * // Search for "morning" and print the top 10 status messages
 * reader.search("morning").foreach {
 *   case (status, score) => println(score + ": " + status.getText)
 * }
 * }}}
 *
 * Without arguments, [[StatusWriterFactory]] creates an index in RAM. To get a
 * reader and a writer for an index in RAM:
 *
 * {{{
   val writer = StatusWriterFactory()
   val reader = StatusReaderFactory(writer.index)
 * }}}
 *
 * ==Search Query Syntax==
 *
 * Other fields can be referenced in the search query in addition to the text
 * of the tweet. For example, the query `"user_screen_name:bob AND morning"`
 * searches for tweets containing "morning" in the text (the default search
 * field) where "bob" appears in the author's screen name. See the source code
 * of the [[StatusToDocument]] object to see which fields are index (only
 * "text" and "user_screen_name" at the time of this writing, but additional
 * fields can easily be added). See the
 * [[http://lucene.apache.org/core/4_2_0/queryparser/org/apache/lucene/queryparser/classic/package-summary.html Lucene query parser format]]
 * for more details on search queries.
 *
 * ==Extension and Customization==
 *
 * Only `Status` objects are currently supported for indexing, but it is
 * somewhat straightforward to add other objects as long as the are
 * serializable. The easiest way to do this is to copy and modify the source file
 * `tshrdlu/twitter/index/Status.scala`.
 *
 * Lucene uses "analyzers" to tokenize text before indexing it. The default
 * English analyzer is selected by this code, but the analyzer as well as other
 * Lucene options can be customized if you are willing to write more code and
 * dig into the [[tshrdlu.util.index]] package.
 *
 */
package object index {}
