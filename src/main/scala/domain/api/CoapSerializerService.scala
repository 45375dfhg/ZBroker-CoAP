package domain.api

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

import domain.model.coap._
import domain.model.coap.body._
import domain.model.coap.body.fields._
import domain.model.coap.header._
import domain.model.coap.header.fields._
import utility.Extractor
import utility.Extractor._
import zio._

/**
 * This service provides functionality to transform an internal representation of a CoAP message
 * into its respective low level form. The result type is of Chunk[Byte] and thus can be
 * transported to third parties via a ZIO-NIO channel
 */
object CoapSerializerService {

  def serializeMessage(message: CoapMessage): Chunk[Byte] =
    serializeHeader(message.header) ++ serializeBody(message.body)

  /**
   * Generates the parameters of the first header byte and prepends them to the parameters of the second
   * byte and lastly to the Chunk that contains the two bytes of the ID.
   *
   * While Chunks are implemented via Arrays they are implemented as Conc-Trees:
   * Their prepend complexity is 0(1).
   */
  private def serializeHeader(head: CoapHeader): Chunk[Byte] =
    (((head.coapVersion.value << 6) + (head.coapType.value << 4) + head.coapTokenLength.value) +:
      ((head.coapCodePrefix.value << 5) + head.coapCodeSuffix.value) +:
        generateMessageId(head.coapId)).map(_.toByte)

  // TODO: WARNING: TOKEN MODEL NOT DONE AS OF NOW
  /**
   * Sequentially transforms the token, the list of options and the payload into their respective
   * byte representation (inside of Chunks) and concatenates them as return value.
   */
  private def serializeBody(body: CoapBody): Chunk[Byte] = {

    def serializeToken(tokenO: Option[CoapToken]): Chunk[Byte] =
      tokenO match {
        case Some(token) => token.value.toChunk
        case None        => Chunk.empty
      }

    /**
     * Transforms all provides internal representations of CoapOptions
     * into a Chunk of Byte. Since the internal rep. is checked on creation
     * for correction, this transformation assumes correct values.
     */
    def serializeAllOptions(optionsO: Option[CoapOptionList]): Chunk[Byte] = {

      /**
       * Forms the header as a singular value and prepends it possible Chunks of extended delta and length
       * values as well as the related option value.
       */
      def serializeOneOption(option: CoapOption): Chunk[Byte] =
        (getHeaderByte(option.coapOptionDelta) + getHeaderByte(option.coapOptionLength)).toByte +:
          (getExtensionFrom(option.coapOptionDelta) ++ getExtensionFrom(option.coapOptionLength) ++
            getOptionValueFrom(option.coapOptionValue))

      optionsO.fold(Chunk[Byte]())(_.value.toChunk.flatMap(serializeOneOption))
    }

    // TODO: IMPLEMENT the other payload types if necessary?
    def serializePayload(payloadO: Option[CoapPayload]): Chunk[Byte] =
      payloadO match {
        case Some(payload) => payload match {
          case p : TextCoapPayload => Chunk.fromArray(p.value.map(_.toByte).toArray)
          case _ => Chunk[Byte]()
        }
        case None          => Chunk[Byte]()
      }

    serializeToken(body.token) ++ serializeAllOptions(body.options) ++ serializePayload(body.payload)
  }

  def getHeaderByte[A : Extractor](optionHeaderParam: A): Int =
    optionHeaderParam.extract match {
      case v if 0   to 12    contains v => v
      case v if 13  to 268   contains v => 13
      case v if 269 to 65804 contains v => 14
      case _                            => 15
    }

  def getExtensionFrom[A : Extractor](optionHeaderParam: A): Chunk[Byte] =
    optionHeaderParam.extract match {
      case v if 0   to 12    contains v => Chunk.empty
      case v if 13  to 268   contains v => Chunk((v - 13).toByte)
      case v if 269 to 65804 contains v => Chunk((((v - 269) >> 8) & 0xFF).toByte, ((v - 269) & 0xFF).toByte)
      case _                            => Chunk.empty // TODO: value can't be higher than 65804 or lower than 0!
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
    case c : StringCoapOptionValueContent  => Chunk.fromArray(c.value.getBytes(StandardCharsets.UTF_8))
    case c : OpaqueCoapOptionValueContent  => c.value
    case EmptyCoapOptionValueContent       => Chunk.empty
    case UnrecognizedValueContent          => Chunk.empty
  }

  // TODO: Refactor this and other similar functions into a utility package!
  private def generateMessageId(id: CoapId): Chunk[Int] = Chunk((id.value >> 8) & 0xFF, id.value & 0xFF)
}
