package dev.openrs2.buffer

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import java.util.zip.CRC32

public fun ByteBuf.readShortSmart(): Int {
    val peek = getUnsignedByte(readerIndex()).toInt()
    return if ((peek and 0x80) == 0) {
        readUnsignedByte().toInt() - 0x40
    } else {
        (readUnsignedShort() and 0x7FFF) - 0x4000
    }
}

public fun ByteBuf.writeShortSmart(v: Int): ByteBuf {
    when (v) {
        in -0x40..0x3F -> writeByte(v + 0x40)
        in -0x4000..0x3FFF -> writeShort(0x8000 or (v + 0x4000))
        else -> throw IllegalArgumentException()
    }

    return this
}

public fun ByteBuf.readUnsignedShortSmart(): Int {
    val peek = getUnsignedByte(readerIndex()).toInt()
    return if ((peek and 0x80) == 0) {
        readUnsignedByte().toInt()
    } else {
        readUnsignedShort() and 0x7FFF
    }
}

public fun ByteBuf.writeUnsignedShortSmart(v: Int): ByteBuf {
    when (v) {
        in 0..0x7F -> writeByte(v)
        in 0..0x7FFF -> writeShort(0x8000 or v)
        else -> throw IllegalArgumentException()
    }

    return this
}

public fun ByteBuf.readIntSmart(): Int {
    val peek = getUnsignedByte(readerIndex()).toInt()
    return if ((peek and 0x80) == 0) {
        readUnsignedShort() - 0x4000
    } else {
        (readInt() and 0x7FFFFFFF) - 0x40000000
    }
}

public fun ByteBuf.writeIntSmart(v: Int): ByteBuf {
    when (v) {
        in -0x4000..0x3FFF -> writeShort(v + 0x4000)
        in -0x40000000..0x3FFFFFFF -> writeInt(0x80000000.toInt() or (v + 0x40000000))
        else -> throw IllegalArgumentException()
    }

    return this
}

public fun ByteBuf.readUnsignedIntSmart(): Int {
    val peek = getUnsignedByte(readerIndex()).toInt()
    return if ((peek and 0x80) == 0) {
        readUnsignedShort()
    } else {
        readInt() and 0x7FFFFFFF
    }
}

public fun ByteBuf.writeUnsignedIntSmart(v: Int): ByteBuf {
    when (v) {
        in 0..0x7FFF -> writeShort(v)
        in 0..0x7FFFFFFF -> writeInt(0x80000000.toInt() or v)
        else -> throw IllegalArgumentException()
    }

    return this
}

public fun ByteBuf.crc32(): Int {
    return crc32(readerIndex(), readableBytes())
}

public fun ByteBuf.crc32(index: Int, len: Int): Int {
    if (index < 0 || index >= capacity() || len < 0 || (index + len) >= capacity()) {
        throw IndexOutOfBoundsException()
    }

    val crc = CRC32()
    val count = nioBufferCount()

    when {
        hasArray() -> crc.update(array(), arrayOffset() + index, len)
        count > 1 -> {
            for (b in nioBuffers(index, len)) {
                crc.update(b)
            }
        }
        count == 1 -> crc.update(nioBuffer(index, len))
        else -> crc.update(ByteBufUtil.getBytes(this, index, len))
    }

    return crc.value.toInt()
}
