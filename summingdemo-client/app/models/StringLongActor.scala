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

package models

import scala.util.Random
import com.twitter.summingdemo.Storage
import com.twitter.summingbird.store.ClientStore
import com.twitter.storehaus.FutureOps
import com.twitter.storehaus.ReadableStore

import akka.actor.Actor
import play.api.libs.iteratee.Concurrent
import play.api.libs.json.{ JsNumber, JsObject, JsString, JsValue }

import Storage.batcher, ReadableStore.enrich

object StringLongActor {
  def store: ReadableStore[String, Long] = ClientStore(Storage.stringLongStore, 3)
}

case class StringLongActor(example: Example, keys: Set[String]) extends Actor {
  val (enumerator, channel) = Concurrent.broadcast[JsValue]

  lazy val store = StringLongActor.store

  def receive = {
    case Connect(host) => sender ! Connected(enumerator)
    case Refresh => {
      val timestamp = System.currentTimeMillis
      FutureOps.mapCollect(store.multiGet(keys))
        .foreach { m: Map[String, Option[Long]] =>
        channel.push {
          JsObject(
            Seq(
              "timestamp" -> JsNumber(timestamp),
              "pairs" -> example.transform(m))
          )
        }
      }
    }
  }
}
