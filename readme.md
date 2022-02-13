[![CI Status](https://github.com/dejankos/java-hash-queue/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/dejankos/java-hash-queue/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/dejankos/java-hash-queue/branch/master/graph/badge.svg)](https://codecov.io/gh/dejankos/java-hash-queue)

# Java Blocking Hash Queue

A hash based thread safe implementation of linked blocking queue with FIFO (first-in-first-out) order.
This data structure is suitable to be used as a blocking queue while having search performance of a hash table.

## Examples

```java
var queue = new LinkedBlockingHashQueue<Integer>();
queue.put(42);

/* 
 * Both calls will have hash table performance instad of linear as usually queues have.
 * Avg: O(1)
 * Worst: O(N) / O( logN ) depending of bucket type where N is size of bucket 
 */
queue.contains(42);
queue.remove(42);

```

#### More examples under /src/test/java

## License

Java Blocking Hash Queue is licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)