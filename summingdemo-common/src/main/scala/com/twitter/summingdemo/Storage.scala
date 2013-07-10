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
import com.twitter.bijection.{Base64String, Bijection, Injection}
import com.twitter.bijection.netty.Implicits._
import com.twitter.conversions.time._
import com.twitter.finagle.builder.ClientBuilder
import com.twitter.finagle.memcached.KetamaClientBuilder
import com.twitter.finagle.memcached.protocol.text.Memcached
import com.twitter.storehaus.Store
import com.twitter.storehaus.algebra.MergeableStore
import com.twitter.storehaus.memcache.{HashEncoder, MemcacheStore}
import com.twitter.summingbird.batch.{ BatchID, Batcher }
import org.jboss.netty.buffer.ChannelBuffer

/**
  * The Storage object contains functions that make it easy to create
  * instances of a local memcache store. To get Memcached set up on
  * your machine, install with homebrew:

  {{{
  brew install memcached
  }}}

  Now, running "memcached" locally will fire up a Memcached instance.
  You can create a store with `Storage.mergeable[String, Long]("keyPrefix")
  */
object Storage {
  import Serialization._, SketchMapImplicits._

  implicit val batcher = Batcher.ofHours(1)

  /**
    * Hardcoded key for use in Global aggregations.
    */
  val globalKey = "ALL"

  /**
    * Returns a MergeableStore[(String, BatchID), Long] instance for
    * use in the Storm portion of a Summingbird job.
    */
  def stringLongStore: MergeableStore[(String, BatchID), Long] =
    mergeable[(String, BatchID), Long]("wordCount")

  /**
    * Returns a MergeableStore[(String, BatchID), SketchMap[String,
    * DecayedValue]] instance for use in the Storm portion of a
    * Summingbird job.
    */
  def trendStore: MergeableStore[(String, BatchID), SketchMap[String, DecayedValue]] =
    mergeable[(String, BatchID), SketchMap[String, DecayedValue]]("trends")

  /**
    * The remaining functions are helper functions that make it easy
    * to create stores.
    */
  val DEFAULT_TIMEOUT = 1.seconds

  def client = {
    val builder = ClientBuilder()
      .name("memcached")
      .retries(2)
      .tcpConnectTimeout(DEFAULT_TIMEOUT)
      .requestTimeout(DEFAULT_TIMEOUT)
      .connectTimeout(DEFAULT_TIMEOUT)
      .readerIdleTimeout(DEFAULT_TIMEOUT)
      .hostConnectionLimit(1)
      .codec(Memcached())

    KetamaClientBuilder()
      .clientBuilder(builder)
      .nodes("localhost:11211")
      .build()
  }

  /**
   * Returns a function that encodes a key to a Memcache key string given a
   * unique namespace string.
   */
  def keyEncoder[T](namespace: String)
    (implicit inj: Codec[T]): T => String = { key: T =>
    def concat(bytes: Array[Byte]): Array[Byte] =
      namespace.getBytes ++ bytes

    (inj andThen (concat _) andThen HashEncoder()
      andThen Bijection.connect[Array[Byte], Base64String])(key).str
  }

  def store[K: Codec, V: Codec](keyPrefix: String): Store[K, V] = {
    implicit val valueToBuf = Injection.connect[V, Array[Byte], ChannelBuffer]
    MemcacheStore(client)
      .convert(keyEncoder[K](keyPrefix))
  }

  def mergeable[K: Codec, V: Codec: Monoid](keyPrefix: String): MergeableStore[K, V] =
    MergeableStore.fromStore(store[K, V](keyPrefix))
}
