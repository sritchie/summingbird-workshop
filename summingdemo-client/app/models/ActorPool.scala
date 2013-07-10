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

import akka.actor.{ ActorRef, Props }
import akka.pattern.ask
import akka.util.Timeout
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.concurrent.Akka
import play.api.libs.iteratee.{Enumerator, Iteratee}
import play.api.libs.json.JsValue
import scala.collection.mutable.{ HashMap => MutableMap, SynchronizedMap }
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

case class Refresh()
case class Connect(example: Example)
case class Connected(enumerator: Enumerator[JsValue])

object ActorPool {
  implicit val timeout = Timeout(5 second)

  val actors: MutableMap[(Example, Set[String]), ActorRef] =
    new MutableMap[(Example, Set[String]), ActorRef]()
        with SynchronizedMap[(Example, Set[String]), ActorRef]

  /**
    * Creates a new actor
    */
  def actor(example: Example, keys: Set[String]): ActorRef = {
    actors.getOrElseUpdate((example, keys), {
      val actor =
        Akka.system.actorOf(
          Props(StringLongActor(example, keys)),
          name = (example.description + keys.mkString(","))
        )
      Akka.system.scheduler.schedule(0.seconds, 3.second, actor, Refresh)
      actor
    })
  }

  def attach(example: Example, keys: Set[String])
      : Future[(Iteratee[JsValue, _], Enumerator[JsValue])] = {
    (actor(example, keys) ? Connect(example)).map {
      case Connected(enumerator) => (Iteratee.ignore[JsValue], enumerator)
    }
  }
}
