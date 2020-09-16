package domain.api

import java.nio.ByteBuffer

import domain.model.coap._
import domain.model.coap.header._
import domain.model.coap.option._

import utility.Extractor
import utility.Extractor._

import zio.Chunk

/**
 * This service provides functionality to transform an internal representation of a CoAP message
 * into its respective low level form. The result type is of Chunk[Byte] and thus can be
 * transported to third parties via a ZIO-NIO channel
 */
object CoapSerializerService {

  def generateFromMessage(message: CoapMessage): Chunk[Byte] =
    generateHeader(message.header) ++ generateBody(message.body)

  /**
   * Generates the parameters of the first header byte and prepends them to the parameters of the second
   * byte and lastly to the Chunk that contains the two bytes of the ID.
   *
   * While Chunks are implemented via Arrays they are implemented as Conc-Trees:
   * Their prepend complexity is 0(1).
   */
  private def generateHeader(head: CoapHeader): Chunk[Byte] = {
    (((head.version.value << 6) + (head.msgType.value << 4) + head.tLength.value) +:
      ((head.cPrefix.value << 5) + head.cSuffix.value) +:
        generateMessageId(head.msgID)).map(_.toByte)
  }

  // TODO: WARNING: TOKEN MODEL NOT DONE AS OF NOW
  /**
   * Sequentially transforms the token, the list of options and the payload into their respective
   * byte representation (inside of Chunks) and concatenates them as return value.
   */
  private def generateBody(body: CoapBody): Chunk[Byte] = {

    /**
     * Transforms all provides internal representations of CoapOptions
     * into a Chunk of Byte. Since the internal rep. is checked on creation
     * for correction, this transformation assumes correct values.
     */
    def generateAllOptions(list: Chunk[CoapOption]): Chunk[Byte] = {

      /**
       * Forms the header as a singular value and prepends it possible Chunks of extended delta and length
       * values as well as the related option value.
       */
      def generateOneOption(option: CoapOption): Chunk[Byte] =
        (option.delta.value + option.length.value).toByte +:
          (getExtensionFrom(option.exDelta) ++ getExtensionFrom(option.exLength) ++ getOptionValueFrom(option.optValue))

      // TODO: rewrite line above
      Chunk.fromArray(list.toArray).flatMap(generateOneOption)
    }

    // TODO: IMPLEMENT the other payload types
    def generatePayload(payload: CoapPayload): Chunk[Byte] = payload match {
      case p : TextCoapPayload => Chunk.fromArray(p.value.map(_.toByte).toArray)
      case _ => Chunk[Byte]()
    }

    (body.token match {
      case Some(t) => t.value
      case None => Chunk.empty
    }) ++ (body.options match {
      case Some(opts) => generateAllOptions(opts)
      case None => Chunk.empty
    }) ++ (body.payload match {
      case Some(pay) => generatePayload(pay)
      case None => Chunk.empty
    })
  }

  // def generateAsByte[A : Extractor](param: A): Int = param.extract

  /**
   * Option Delta and Option Length can but most must be extended. If the internal representation of an Option contain
   * an extended value that value might be of size one or two Bytes. This functions returns the respective Chunk[Byte]
   * presentation of the saved Integer value.
   *
   * @param opt An Option of an Extractor member.
   * @tparam A An Extractor type class member - all members are value classes,
   *           thus they hold a single extractable integer
   * @return A Chunk[Byte] of either length 1 or 2.
   */
  private def getExtensionFrom[A : Extractor](opt: Option[A]): Chunk[Byte] =
    opt.fold(Chunk[Byte]()) { e =>
      if (e.extract < 269) Chunk(e.extract.toByte)
      else Chunk(((e.extract >> 8) & 0xFF).toByte, (e.extract & 0xFF).toByte)
    }

  /**
   * Converts the internal representation of a single Option Value into a Chunk[Byte] ready for outward traffic.
   *
   * @param v an internal representation of an OptionValue that can either hold an Int, a String, a Chunk[Byte]
   *          or simply be empty.
   * @return The Chunk[Byte] equivalent of the given CoapOptionValue
   */
  private def getOptionValueFrom(v: CoapOptionValue): Chunk[Byte] = v.content match {
    case c : IntCoapOptionValueContent     => Chunk.fromByteBuffer(ByteBuffer.allocate(4).putInt(c.value).compact)
    case c : StringCoapOptionValueContent  => Chunk.fromArray(c.value.map(_.toByte).toArray)
    case c : OpaqueCoapOptionValueContent  => c.value
    case EmptyCoapOptionValueContent       => Chunk.empty
    case UnrecognizedValueContent          => Chunk.empty
  }

  // TODO: Refactor
  private def generateMessageId(id: CoapId): Chunk[Int] = Chunk((id.value >> 8) & 0xFF, id.value & 0xFF)
}
