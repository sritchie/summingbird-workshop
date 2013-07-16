/*
 Copyright 2013 Twitter, Inc.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package com.twitter.summingdemo

import com.twitter.algebird._
import com.twitter.conversions.time._
import com.twitter.util.Duration
import com.twitter.summingbird._
import com.twitter.summingbird.batch.Batcher
import twitter4j.{ Status, TwitterStreamFactory }
import twitter4j.conf.ConfigurationBuilder
import scala.collection.JavaConverters._
import java.lang.{ Double => JDouble, Integer => JInt }

object Exercises {
  import SketchMapImplicits.{ trendMonoid, decayedMonoid }

  /**
    * The TimeExtractor is required for batch/realtime jobs.
    */
  implicit val timeOf: TimeExtractor[Status] =
    TimeExtractor(_.getCreatedAt.getTime)

  /**
    * Splits up a sentence on whitespace and returns a sequence of
    * lowercased words.
    */
  def tokenize(text: String) : TraversableOnce[String] =
    text.toLowerCase
      .replaceAll("[^a-zA-Z0-9\\s]", "")
      .split("\\s+")

  def tweetCount[P <: Platform[P]](
    source: Producer[P, Status],
    store: P#Store[String, Long]) =
    sys.error(
      """Write a job that calculates a count of ALL tweets that have come
         through the system."""
    )

  def wordCount[P <: Platform[P]](
    source: Producer[P, Status],
    store: P#Store[String, Long]) =
    sys.error(
      """Write a job that generates key-value pairs of word -> count."""
    )

  def letterCount[P <: Platform[P]](
    source: Producer[P, Status],
    store: P#Store[String, Long]) =
    sys.error(
      """Expand on the job above by calculating the count for each
         LETTER. This will be an interesting demo of the Ratios
         feature of the UI."""
    )

  def tweetCountWithCustomMonoid[P <: Platform[P]](
    source: Producer[P, _]
  )(fn: Monoid[Long] => P#Store[String, Long]) = {
    sys.error(
      """Write a job that looks like the tweetCount example above, but uses
         a custom monoid that resets the count every 500 tweets."""
    )
  }

  val halfLife = 2.hours.inMillis

  // Helper function to create a DecayedValue instance.
  def decayedValue(timestamp: Long) =
    DecayedValue.build(1L, timestamp, halfLife)

  // Helper function to create a sketchMap instance.
  def sketchMap(k: String, timestamp: Long) =
    trendMonoid.create(k -> decayedValue(timestamp))

  def trendJob[P <: Platform[P]](
    source: Producer[P, Status],
    store: P#Store[String, SketchMap[String, DecayedValue]]) =
    sys.error(
      """Design a job which produces, for some set of keys, a SketchMap of
         hashtag to decayed value. The functions above should help in
         the actual writing."""
    )
}
