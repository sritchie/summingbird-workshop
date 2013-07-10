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
import com.twitter.bijection._
import com.twitter.chill.{ KryoImplicits, KryoBijection, KryoInjection }
import Serialization.Codec
import scala.util.control.Exception.catching

/**
  * This is advanced stuff. The following objects make it easy to
  * create instances of a SketchMap, which is a more generalized
  * version of the CountMinSketch data structure.
  */

/**
  * Convenience class that holds the configuration parameters
  * necessary to create a SketchMap monoid.
  */
case class SketchMapConfig(
  eps: Double,
  delta: Double,
  seed: Int,
  heavyHittersCount: Int
)

object SketchMapConfig {
  /**
    * Factory that creates SketchMap monoids based on SketchMap
    * configuration case classes.
    */
  implicit def toMonoid[K, V](config: SketchMapConfig)
    (implicit inj: Codec[K], ordering: Ordering[V], monoid: Monoid[V])
      : SketchMapMonoid[K, V] = {
    SketchMap.monoid[K, V](
      config.eps, config.delta, config.seed,
      config.heavyHittersCount
    )(inj, ordering, monoid)
  }
}

object SketchMapImplicits {
  import KryoImplicits.toRich //Add methods to Kryo
  import Serialization._

  /**
    * Returns a new SketchMapMonoid that uses an implicitly supplied
    * Codec[K], Ordering[V] and Monoid[V].
    */
  def monoid[K: Codec, V: Ordering: Monoid]: SketchMapMonoid[K, V] =
    SketchMapConfig(
      eps = 0.01, delta = 0.01, seed = 1,
      heavyHittersCount = 20
    )

  /**
    * This particular Monoid is pulled in implicitly by the
    * trendMonoid below.
    */
  implicit val decayedMonoid: Monoid[DecayedValue] =
    DecayedValue.monoidWithEpsilon(0.01)

  /**
    * Monoid for a SketchMap data structure that uses a DecayedValue
    * as its value.
    */
  implicit val trendMonoid: SketchMapMonoid[String, DecayedValue] =
    monoid[String, DecayedValue]

  /**
    * DO NOT USE THIS TO STORE PRODUCTION DATA! This KryoBijection is
    * capable of serializing SketchMap instances, and only required
    * because SketchMap is a pain in the ass to serialize without some
    * help.
    */
  val kryoBij = new KryoBijection {
    override def getKryo = super.getKryo
      .injectionForClass[SparseVector[Any]]
      .injectionForClass[DenseVector[Any]]
  }

  /**
    */
  def kryoInjection[T]: Injection[T, Array[Byte]] =
    Injection.build { t: T => kryoBij(t.asInstanceOf[AnyRef]) } { bytes =>
      catching(classOf[Exception]).opt(kryoBij.invert(bytes).asInstanceOf[T])
    }

  implicit def sparseToTriple[T]: Bijection[SparseVector[T], (Int, T, Map[Int, T])] =
    Bijection.build { v: SparseVector[T] =>
      (v.size, v.sparseValue, v.map)
    } { case (size, sparseV, m) =>
        SparseVector(m, sparseV, size)
    }

  implicit def denseToTriple[T]: Bijection[DenseVector[T], (Int, T, Vector[T])] =
    Bijection.build { v: DenseVector[T] =>
      (v.size, v.sparseValue, v.iseq)
    } { case (size, sparseV, v) =>
        DenseVector(v, sparseV, size)
    }

  implicit def sparseToBytes[T]: Injection[SparseVector[T], Array[Byte]] =
    sparseToTriple.andThen(kryoInjection[(Int, T, Map[Int, T])])

  implicit def denseToBytes[T]: Injection[DenseVector[T], Array[Byte]] =
    denseToTriple.andThen(kryoInjection[(Int, T, Vector[T])])

  /**
    * Finally, the core. This Injection allows storm to serialize SketchMap instances.
    */
  implicit val sketchMapInjection: Injection[SketchMap[String, DecayedValue], Array[Byte]] =
    Injection.build { m: SketchMap[String, DecayedValue] =>
      kryoInjection(m)
    } { bytes =>
      kryoInjection.invert(bytes)
        .asInstanceOf[Option[SketchMap[String, DecayedValue]]]
    }
}

/**
  * Quick test of the SketchMap serialization:
  {{{
  import com.twitter.algebird.{ DecayedValue, SketchMap }
  import com.twitter.conversions.time._
  import com.twitter.util.Duration
  import com.twitter.summingbird._
  import com.twitter.summingbird.batch.Batcher
  import twitter4j.{ Status, TwitterStreamFactory }
  import twitter4j.conf.ConfigurationBuilder
  import scala.collection.JavaConverters._
  import java.lang.{ Double => JDouble, Integer => JInt }
  import com.twitter.summingdemo._
  import com.twitter.algebird._
  import com.twitter.bijection._
  import SketchMapImplicits.{ trendMonoid, decayedMonoid }
  import com.twitter.chill._
  import KryoImplicits.toRich //Add methods to Kryo

  DecayedValue.build(1L, 100L, 100L)
  val v = trendMonoid.create("face" -> DecayedValue.build(1L, 100L, 100L))
  val bytes = SketchMapImplicits.sketchMapInjection(v)
  SketchMapImplicits.sketchMapInjection.invert(bytes)

  val store = Storage.trendStore
  store.get("ALL" -> Storage.batcher.currentBatch).get.get.heavyHitters.foreach { case (k, v) => println(k) }

  def try(s: String) = {
  store.get("ALL" -> Storage.batcher.currentBatch).get.get.heavyHitters.sortBy(_._2.value).reverse.take(5).map(_._1).foreach(println)
  }
  }}}
  */
