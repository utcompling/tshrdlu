package tshrdlu.twitter

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
import twitter4j.FilterQuery

/**
 * A trait for classes that can create FilterQuery objects.
 */
trait Filterable {
  /**
   * Set up the query based on the provided arguments.
   */
  def getQuery(args: Array[String]): FilterQuery
}

/**
 * A filter  based on the provided user ids.
 */
trait IdFilter extends Filterable {
  def getQuery(args: Array[String]) = QueryBuilder.follow(args)
}

/**
 * A filter based on the provided terms appearing in a tweet.
 */
trait TermFilter extends Filterable {
  def getQuery(args: Array[String]) = QueryBuilder.track(args)
}

/**
 * A filter that restricts interest to locations contained within
 * the provided bounding boxes.
 */
trait LocationFilter extends Filterable {
  def getQuery(args: Array[String]) = QueryBuilder.locations(args)
}


/**
 * A helper object with useful functions for constructing various
 * types of FilterQuery objects. Basically, these are just wrappers
 * to the construct+method style used by FilterQuery, plus helper
 * functions that turn Array[String] inputs into the structure and
 * types needed for constructing FilterQueries.
 */
object QueryBuilder {

  /**
   * Follow the user ids indicated in the Array of Longs.
   */
  def follow(ids: Array[Long]): FilterQuery = 
    new FilterQuery(ids)

  /**
   * Turn Strings into Longs and call follow/1.
   */
  def follow(idStrings: Array[String]): FilterQuery = 
    follow(idStrings.map(_.toLong))

  /**
   * Track the terms provided.
   */
  def track(terms: Array[String]) = 
    new FilterQuery().track(terms)

  /**
   * Track the locations provided.
   */
  def locations(boxes: Array[Array[Double]]): FilterQuery = 
    new FilterQuery().locations(boxes)

  /**
   * Convert a flat Array of Strings into an Array of two-element
   * Arrays of Doubles (each a latitude/longitude value), and
   * then call locations/1.
   */
  def locations(flatStringValues: Array[String]): FilterQuery =
    locations(flatStringValues.map(_.toDouble).grouped(2).toArray)

}
