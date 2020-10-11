
import domain.model.coap.header._

import zio.random.Random
import zio.test._
import zio.test.Assertion._

import zio._

object CoapModelSpec extends DefaultRunnableSpec {

  def fv(v: Int = 1): Int = v << 6
  def ft(v: Int): Int = v << 4
  def fl(v: Int) = v

  def pf(v: Int): Int = v << 5
  def sf(v: Int): Int = v

  def f1(v: Int, t: Int, l: Int): Int = fv(v) + ft(t) + fl(l)
  def f2(p: Int, s: Int): Int = pf(p) + sf(s)

  def idf(id: Int): Chunk[Int] = Chunk((id >> 8) & 0xFF, id & 0xFF)

  val generateByteMessage: Gen[Random, Chunk[Byte]] =
    for {
      v <- Gen.int(1, 1)
      t <- Gen.int(0, 3)
      l <- Gen.int(0, 8)
      p <- Gen.int(0, 7)
      s <- Gen.int(0, 31)
      i <- Gen.int(0, 65535)
    } yield (f1(v,t,l) +: (f2(p,s) +: idf(i))).map(_.toByte)

  val reset = Chunk(112.toByte, 0.toByte) ++ idf(65535).map(_.toByte)
  val ackno = Chunk(96.toByte,  0.toByte) ++ idf(65535).map(_.toByte)

  val wrongProtocol     = Chunk(240.toByte, 0.toByte) ++ idf(65535).map(_.toByte)
  val wrongHeaderLength = Chunk(112.toByte, 0.toByte)


  val testEnvironment = Random.live ++ Sized.live(1)

  override def spec =
    suite("Header")(
      testM("correctly generate N message from random valid chunks") {
        checkM(Gen.listOfN(100)(generateByteMessage))(messages => ZIO.foreach(messages)(CoapHeader.fromDatagram).as(assertCompletes))
      },
      testM("test reset header value")(CoapHeader.fromDatagram(reset).as(assertCompletes)),
      testM("test reset header value")(CoapHeader.fromDatagram(ackno).as(assertCompletes)),
      testM("wrong protocol")(CoapHeader.fromDatagram(wrongProtocol).flip.as(assertCompletes)),
    ).provideCustomLayerShared(testEnvironment)

}

// testM("test reset and acknowledge header values")(CoapHeader.fromDatagram(reset).as(assertCompletes))