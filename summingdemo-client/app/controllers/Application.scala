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

package controllers

import play.api._
import play.api.mvc._
import models.{ ActorPool, Example }
import play.api.libs.json.{ JsArray, Json, JsValue, JsObject, JsString }
import play.api.libs.iteratee.Iteratee
import play.api.libs.iteratee.Enumerator
import play.api.libs.concurrent.Promise
import scala.concurrent.duration.DurationInt
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object Application extends Controller {
  /**
    * The main action. Loads up a list of examples.
    */
  def index = Action {
    Ok(views.html.dashboard("Summingbird Tutorials", Example.examples))
  }

  def example(id: String, keyString: String) = Action { implicit request =>
    Example.examples.find(_._1 == id) match {
      case Some((id, _)) => Ok(views.html.example(id, keyString))
      case None => NoContent
    }
  }

  def initialize(id: String, keyString: String) = {
    val keys = keyString.split(",").toSet
    WebSocket.async[JsValue] { request =>
      Example.examples.find(_._1 == id) match {
        case Some((_, example)) => ActorPool.attach(example, keys)
        case None => {
          val enumerator =
            Enumerator.generateM[JsValue](Promise.timeout(None, 1.second))
              .andThen(Enumerator.eof)
          Promise.pure(Iteratee.ignore[JsValue] -> enumerator)
        }
      }
    }
  }
}
