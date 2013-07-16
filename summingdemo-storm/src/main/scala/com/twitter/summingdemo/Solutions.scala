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
import twitter4j.Status
import scala.collection.JavaConverters._

object Solutions {
  import SketchMapImplicits.{ trendMonoid, decayedMonoid }

  /**
    * These two items are required to run Summingbird in
    * batch/realtime mode, across the boundary between storm and
    * scalding jobs.
    */
  implicit val timeOf: TimeExtractor[Status] =
    TimeExtractor(_.getCreatedAt.getTime)

  def tokenize(text: String) : TraversableOnce[String] =
    text.toLowerCase
      .replaceAll("[^a-zA-Z0-9\\s]", "")
      .split("\\s+")

  /**
    * Write a job that calculates a count of ALL tweets that have come
    * through the system.
    */
  def wordCount[P <: Platform[P]](
    source: Producer[P, Status],
    store: P#Store[String, Long]) =
    source
      .flatMap { tweet: Status => tokenize(tweet.getText).map(_ -> 1L) }
      .sumByKey(store)

  /**
    * Write a job that generates key-value pairs of word -> count.
    */
  def letterCount[P <: Platform[P]](
    source: Producer[P, Status],
    store: P#Store[String, Long]) =
    source.flatMap { tweet: Status =>
      tokenize(tweet.getText).flatMap(_.toSeq.map(_.toString -> 1L))
    }.sumByKey(store)

  /**
    * Expand on the job above by calculating the count for each
    * LETTER. This will be an interesting demo of the Ratios feature
    * of the UI.
    */
  def tweetCount[P <: Platform[P]](
    source: Producer[P, Status],
    store: P#Store[String, Long]) =
    source
      .map(_ => Storage.globalKey -> 1L)
      .sumByKey(store)

  /**
    * Write a job that looks like the tweetCount example above, but uses
    * a custom monoid that resets the count every 500 tweets.
    */
  def tweetCountWithCustomMonoid[P <: Platform[P]](
    source: Producer[P, Status]
  )(fn: Monoid[Long] => P#Store[String, Long]) = {
    val monoid: Monoid[Long] = new Monoid[Long] {
      val zero = 0L
      override def plus(l: Long, r: Long): Long = {
        val sum = l + r
        if (sum > 500)
          zero
        else
          sum
      }
    }
    source
      .map(_ => Storage.globalKey -> 1L)
      .sumByKey(fn(monoid))
  }

  /**
    * Design a job which produces, for some set of keys, a SketchMap
    * of hashtag to decayed value. The functions above should help in
    * the actual writing.
    */
  def trendJob[P <: Platform[P]](
    source: Producer[P, Status],
    store: P#Store[String, SketchMap[String, DecayedValue]]) =
    source
      .flatMap { tweet: Status =>
      for {
        entity <- tweet.getHashtagEntities.toSeq
        hashTag = entity.getText
        key <- Seq(Some("ALL"), Option(tweet.getPlace).map(_.getCountryCode)).flatten
      } yield key -> Exercises.sketchMap(hashTag, tweet.getCreatedAt.getTime)
    }.sumByKey(store)
}
