package utility

import zio.Chunk

object ByteUtility {

  val mask = 0xff

  def int2ByteChunk(i: Int): Chunk[Byte] = {
    Chunk(
      ((i >> 24) & mask).toByte,
      ((i >> 16) & mask).toByte,
      ((i >>  8) & mask).toByte,
      ((i >>  0) & mask).toByte,
    )
  }
}