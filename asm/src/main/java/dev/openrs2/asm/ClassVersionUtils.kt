package dev.openrs2.asm

object ClassVersionUtils {
    private fun swapWords(v: Int): Int {
        return (v shl 16) or (v ushr 16)
    }

    fun maxVersion(v1: Int, v2: Int): Int {
        return if (swapWords(v1) > swapWords(v2)) {
            v1
        } else {
            v2
        }
    }
}