import zio.Chunk

import scala.collection.mutable


val a = (0 to 10).map(a => a)
val b = (5 to 15).map(a => a)

val arrA = Array(a: _*)
val arrB = Array(b: _*)

val chunkA = Chunk(a: _*)
val chunkB = Chunk(b: _*)

val setA = Set(a: _*)
val setB = Set(b: _*)

val bitA = mutable.BitSet(a: _*)
val bitB = mutable.BitSet(b: _*)

for (i <- 1 to 1000000) {
  i
}


val t1 = System.nanoTime

(arrA ++ arrB).distinct

val t2 = System.nanoTime

(chunkA ++ chunkB).distinct

val t3 = System.nanoTime

(setA union setB)

val t4 = System.nanoTime

(bitA union bitB)

val t5 = System.nanoTime

t2 - t1
t3 - t2
t4 - t3
t5 - t4


