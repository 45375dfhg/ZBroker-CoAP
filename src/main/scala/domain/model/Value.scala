package domain.model

import zio.nio.core.ByteBuffer
import zio.Chunk

/*
    Value Classes 
*/

class ByteBufferWrapper(val underlying: ByteBuffer) extends AnyVal

class ChunkByteWrapper(val underlying: Chunk[Byte]) extends AnyVal

class RequestWrapper(val underlying: Int) extends AnyVal // placeholder

