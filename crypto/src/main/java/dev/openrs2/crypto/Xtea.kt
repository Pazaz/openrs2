package dev.openrs2.crypto

import io.netty.buffer.ByteBuf

private const val GOLDEN_RATIO = 0x9e3779b9.toInt()
private const val ROUNDS = 32

fun ByteBuf.xteaEncrypt(index: Int, length: Int, key: IntArray) {
    require(key.size == 4)

    for (i in index until index + length step 8) {
        var sum = 0
        var v0 = getInt(i)
        var v1 = getInt(i + 4)

        for (j in 0 until ROUNDS) {
            v0 += (((v1 shl 4) xor (v1 ushr 5)) + v1) xor (sum + key[sum and 3])
            sum += GOLDEN_RATIO
            v1 += (((v0 shl 4) xor (v0 ushr 5)) + v0) xor (sum + key[(sum ushr 11) and 3])
        }

        setInt(i, v0)
        setInt(i + 4, v1)
    }
}

fun ByteBuf.xteaDecrypt(index: Int, length: Int, key: IntArray) {
    require(key.size == 4)

    for (i in index until index + length step 8) {
        @Suppress("INTEGER_OVERFLOW")
        var sum = GOLDEN_RATIO * ROUNDS
        var v0 = getInt(i)
        var v1 = getInt(i + 4)

        for (j in 0 until ROUNDS) {
            v1 -= (((v0 shl 4) xor (v0 ushr 5)) + v0) xor (sum + key[(sum ushr 11) and 3])
            sum -= GOLDEN_RATIO
            v0 -= (((v1 shl 4) xor (v1 ushr 5)) + v1) xor (sum + key[sum and 3])
        }

        setInt(i, v0)
        setInt(i + 4, v1)
    }
}