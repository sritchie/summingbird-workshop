# Summingbird Workshop

This repository contains code for the Summingbird Workshop at Lambda Jam 2013.

## Dependencies

To run this code, you'll need to install Memcached and Play. Both are available via homebrew.

- Install Homebrew
* `brew install memcached play`

## Creating a ClientStore at the REPL

To create a client, run `play summingdemo-client/console`, then the following scala commands:

```scala
import com.twitter.summingdemo._
import com.twitter.summingbird.store._
import Storage.batcher

scala> val stringStore = ClientStore(Storage.stringLongStore, 3)
stringStore: com.twitter.summingbird.store.ClientStore[String,Long] = com.twitter.summingbird.store.ClientStore@478674ae

scala> val trendStore = ClientStore(Storage.stringLongStore, 3)
trendStore: com.twitter.summingbird.store.ClientStore[String,Long] = com.twitter.summingbird.store.ClientStore@198ec219
```

You should be able to use these with the examples we'll write during the workshop.

## Running the Play UI

The workshop has a UI written using Play. This UI allows you to query the output of a Summingbird job that produces `[String, Long]`.

First, fire up a Memcached instance with `memcached`. In a new terminal window, run the Play UI with the following command:

```bash
play summingdemo-client/run
```

The UI will be available at [http://localhost:9000](http://localhost:9000). The UI takes a comma-separated list of string keys and graphs the results over time for each key. You can click "Counts" to see the counts for each key, or "Ratios" to see the weight of each key relative to the others.

Now, fire up a local Storm instance like so:

```bash
play summingdemo-storm/"run tweet-count"
```

Test that everything is working by typing "ALL" into the search bar and hitting the "Counts" link. You should see a line rising off to the right.

You can also test the the REPL interaction is working by creating a REPL, as described above:

```scala
// First, run `play summingdemo-client/console` in a shell.

import com.twitter.summingdemo._
import com.twitter.summingbird.store._
import Storage.batcher

scala> val stringStore = ClientStore(Storage.stringLongStore, 3)
stringStore: com.twitter.summingbird.store.ClientStore[String,Long] = com.twitter.summingbird.store.ClientStore@478674ae

scala> stringStore.get("ALL").get
res0: Option[Long] = Some(1674)
```

So good.

## Authors

* Sam Ritchie <https://twitter.com/sritchie>

## License

Copyright 2013 Twitter, Inc.

Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
