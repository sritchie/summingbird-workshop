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

import backtype.storm.Config
import com.twitter.summingbird.kryo.KryoRegistrationHelper
import com.twitter.algebird._
import com.twitter.summingbird._
import com.twitter.summingbird.batch.BatchID
import com.twitter.summingbird.storm.{ MergeableStoreSupplier, Storm }
import com.twitter.tormenta.spout.{ Spout, TwitterSpout }
import twitter4j.TwitterStreamFactory
import twitter4j.conf.ConfigurationBuilder

object ExerciseRunner {
  import StormRunner._, Exercises._
  import Serialization._, Storage.batcher

  /**
    * When this main method is executed, Storm will begin running on a
    * separate thread on the local machine, pulling tweets off of the
    * TwitterSpout, generating and aggregating key-value pairs and
    * merging the incremental counts in the memcache store.
    *
    * Before running this code, make sure to start a local memcached
    * instance with "memcached". ("brew install memcached" will get
    * you all set up if you don't already have memcache installed
    * locally.)
    */
  def main(args: Array[String]) {
    Storm.local(args(0))
      .withConfigUpdater(registrations(_))
      .run(
      args(0) match {
        case "word-count" =>  wordCount[Storm](spout, storeSupplier)
        case "letter-count" => letterCount[Storm](spout, storeSupplier)
        case "tweet-count" => tweetCount[Storm](spout, storeSupplier)
        case "custom-count" =>
          tweetCountWithCustomMonoid[Storm](spout) { implicit mon: Monoid[Long] =>
            MergeableStoreSupplier.from(
              Storage.mergeable[(String, BatchID), Long]("wordCount")
            )
          }
        case "trends" => trendJob[Storm](spout,
          MergeableStoreSupplier.from(Storage.trendStore)
        )
      }
    )
  }
}

object SolutionRunner {
  import StormRunner._, Solutions._
  import Serialization._, Storage.batcher

  /**
    * When this main method is executed, Storm will begin running on a
    * separate thread on the local machine, pulling tweets off of the
    * TwitterSpout, generating and aggregating key-value pairs and
    * merging the incremental counts in the memcache store.
    *
    * Before running this code, make sure to start a local memcached
    * instance with "memcached". ("brew install memcached" will get
    * you all set up if you don't already have memcache installed
    * locally.)
    */
  def main(args: Array[String]) {
    Storm.local(args(0))
      .withConfigUpdater(registrations(_))
      .run(
      args(0) match {
        case "word-count" =>  wordCount[Storm](spout, storeSupplier)
        case "letter-count" => letterCount[Storm](spout, storeSupplier)
        case "tweet-count" => tweetCount[Storm](spout, storeSupplier)
        case "custom-count" =>
          tweetCountWithCustomMonoid[Storm](spout) { implicit mon: Monoid[Long] =>
            MergeableStoreSupplier.from(
              Storage.mergeable[(String, BatchID), Long]("wordCount")
            )
          }
        case "trends" => trendJob[Storm](spout,
          MergeableStoreSupplier.from(Storage.trendStore)
        )
      }
    )
  }
}

object StormRunner {
  import Storage.batcher, Serialization._

  def registrations(conf: Config): Config = {
    KryoRegistrationHelper.registerInjections(
      conf,
      Seq(Serialization.injectionPair(SketchMapImplicits.sketchMapInjection))
    )
    conf
  }

  /**
    * Configuration for Twitter4j. Configuration can also be managed
    * via a properties file, as described here:
    *
    * http://tugdualgrall.blogspot.com/2012/11/couchbase-create-large-dataset-using.html
    */
  lazy val config = new ConfigurationBuilder()
    .setOAuthConsumerKey("")
    .setOAuthConsumerSecret("")
    .setOAuthAccessToken("")
    .setOAuthAccessTokenSecret("")
    .build

  /**
    * "spout" is a concrete Storm source for Status data. This will
    * act as the initial producer of Status instances in the
    * Summingbird word count job.
    */
  val spout = TwitterSpout(new TwitterStreamFactory(config))

  val storeSupplier: MergeableStoreSupplier[String, Long] =
    MergeableStoreSupplier.from(Storage.stringLongStore)
}
