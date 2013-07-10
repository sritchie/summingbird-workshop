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

import play.api.libs.json.{ JsNumber, JsObject }

object Example {
  def ratios[K](m: Map[K, Option[Long]]): Map[K, Float] = {
    val newM: Map[K, Float] = m.map { case (k, v) =>
      k -> v.map(_.toFloat).getOrElse(0.0f)
    }
    val sum = newM.values.foldLeft(0.0)(_ + _)
    if (sum == 0.0)
      newM
    else
      newM.mapValues(_.toDouble / sum * 100 toFloat)
  }

  lazy val examples: List[(String, Example)] =
    List(
      "counter" -> Counter("Counts"),
      "ratio" -> Ratio("Ratios")
    )
}

sealed trait Example {
  def description: String
  def transform(m: Map[String, Option[Long]]): JsObject
}

case class Counter(description: String) extends Example {
  def transform(m: Map[String, Option[Long]]) =
    JsObject {
      m.mapValues(l => JsNumber(l.getOrElse(0L): Long)).toSeq
    }
}
case class Ratio(description: String) extends Example {
  def transform(m: Map[String, Option[Long]]) =
    JsObject {
      Example.ratios(m).mapValues(JsNumber(_)).toSeq
    }
}
