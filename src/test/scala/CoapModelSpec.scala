
import domain.model.coap.CoapMessage
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

  // valid header generator
  val generateHeader: Gen[Random, Chunk[Byte]] =
    for {
      v <- Gen.int(1, 1)
      t <- Gen.int(0, 3)
      l <- Gen.int(0, 8)
      p <- Gen.int(0, 7)
      s <- Gen.int(0, 31)
      i <- Gen.int(0, 65535)
    } yield (f1(v,t,l) +: (f2(p,s) +: idf(i))).map(_.toByte)

  // fixed correct header chunks
  val reset = Chunk(112.toByte, 0.toByte) ++ idf(65535).map(_.toByte)
  val ackno = Chunk(96.toByte,  0.toByte) ++ idf(65535).map(_.toByte)

  // fixed invalid header chunks
  val wrongProtocol     = Chunk(240.toByte, 0.toByte) ++ idf(65535).map(_.toByte)
  val wrongHeaderLength = Chunk(112.toByte, 0.toByte)

  // basic get
  val con_get       = Chunk[Byte](80,1,18,52)
  val con_get_block = Chunk[Byte](64,1,-75,123,-47,10,2)
  val non_get_block = Chunk[Byte](80,1,-75,124,-47,10,2)

  // uri-path get
  val uri_path_get = Chunk[Byte](80,1,-110,-101,-69,46,119,101,108,108,45,107,110,111,119,110,4,99,111,114,101,-63,2)

  // uri-path put with ascii payload
  val uri_path_put_ascii = Chunk[Byte](80,3,-69,-103,-69,46,119,101,108,108,45,107,110,111,119,110,4,99,111,114,101,16,-79,2,-1,103,104,102,103,104,104)

  // basic put with UTF-8 payload (™)
  val put_utf8 = Chunk[Byte](80,3,18,54,-64,-1,-30,-124,-94)

  // missing payload marker
  val put_no_marker = Chunk[Byte](80,3,18,54,-64,-30,-124,-94)

  // marker but no load
  val missing_load = Chunk[Byte](80,3,18,54,-64,-1)

  //
  def constructDeconstructEquality(chunk: Chunk[Byte]) =
    CoapMessage.fromDatagram(chunk).map(_.toByteChunk).map(r => assert(r)(hasSameElements(chunk)))

  val testEnvironment = Random.live ++ Sized.live(1)

  override def spec =
    suite("CoAP Model Serial")(
      suite("CoAP Header")(
        suite("successes")(
          testM("correctly generate 100 headers from random valid chunks") {
            checkM(Gen.listOfN(100)(generateHeader))(messages => ZIO.foreach(messages)(CoapHeader.fromDatagram).as(assertCompletes))
          },
          testM("test reset header value")(CoapHeader.fromDatagram(reset).as(assertCompletes)),
          testM("test reset header value")(CoapHeader.fromDatagram(ackno).as(assertCompletes))),
        suite("failures")(
          testM("wrong protocol detected")(CoapHeader.fromDatagram(wrongProtocol).flip.as(assertCompletes)),
          testM("wrong header length detected")(CoapHeader.fromDatagram(wrongHeaderLength).flip.as(assertCompletes)),
          testM("empty datagram detected")(CoapHeader.fromDatagram(Chunk[Byte]()).flip.as(assertCompletes)),
          testM("right size but empty detected")(CoapHeader.fromDatagram(Chunk[Byte](0, 0, 0, 0)).flip.as(assertCompletes)),
        )
      ),
      suite("CoAP Body")(
        suite("GET")(
          testM("GET CON")(CoapMessage.fromDatagram(con_get).as(assertCompletes)),
          testM("GET CON BLOCK")(CoapMessage.fromDatagram(con_get_block).as(assertCompletes)),
          testM("GET NON BLOCK")(CoapMessage.fromDatagram(non_get_block).as(assertCompletes)),
          testM("uri-path GET")(CoapMessage.fromDatagram(uri_path_get).as(assertCompletes)),
        ),
        suite("PUT")(
          testM("uri-path PUT ascii")(CoapMessage.fromDatagram(uri_path_put_ascii).as(assertCompletes)),
          testM("PUT UTF8") {
            for {
              r <- CoapMessage.fromDatagram(put_utf8).map(_.toByteChunk)
            } yield assert(r)(hasSameElements(put_utf8))
          },
          testM("missing load detected")(CoapMessage.fromDatagram(missing_load).flip.as(assertCompletes)),
        )
      ),
      suite("deserialization")(
        testM("GET CON")(constructDeconstructEquality(con_get)),
        testM("GET CON BLOCK")(constructDeconstructEquality(con_get_block)),
        testM("GET NON BLOCK")(constructDeconstructEquality(non_get_block)),
        testM("uri-path GET")(constructDeconstructEquality(uri_path_get)),
        testM("uri-path PUT ascii")(constructDeconstructEquality(uri_path_put_ascii)),
      )
    ).provideCustomLayerShared(testEnvironment)


}
