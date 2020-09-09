import java.nio.ByteBuffer

import domain.model.coap.CoapOptionNumber
import zio.Chunk

final case class CoapOptionValue[A](
  number: CoapOptionNumber,
  critical: Critical,
  unsafe: Unsafe,
  noCacheKey: NoCacheKey,
  repeatable: Repeatable,
  format: Format[A],
  content: A
)


sealed trait Format[A] {
  def format(input: Chunk[Byte]): A
}
case object IntFormat extends Format[Int] {
  // TODO: Grab the first Byte and convert it?
  override def format(input: Chunk[Byte]): Int = ByteBuffer.wrap(input.toArray).getInt
}

case object EmptyFormat extends Format[Unit] {
  override def format(input: Chunk[Byte]): Unit = Chunk[Byte]()
}

case object StringFormat extends Format[String] {
  // TODO: Is this UTF-8?
  override def format(input: Chunk[Byte]): String = input.map(_.toChar).mkString
}

final case class Critical(value: Boolean) extends AnyVal
final case class Unsafe(value: Boolean) extends AnyVal
final case class NoCacheKey(value: Boolean) extends AnyVal
final case class Repeatable(value: Boolean) extends AnyVal

// (raw, number) => (number => format) => (format => converted)
// converted[byte, string, empty, uint]