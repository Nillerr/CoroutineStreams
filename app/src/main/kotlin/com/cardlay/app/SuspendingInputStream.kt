package com.cardlay.app

import java.io.InputStream

interface SuspendingInputStream : SuspendingCloseable {
    suspend fun read(): Int

    suspend fun read(destination: ByteArray): Int {
        return read(destination, 0, destination.size)
    }

    suspend fun read(destination: ByteArray, offset: Int, length: Int): Int

    suspend fun readAllBytes(): ByteArray {
        val buffer = ByteArray(8192)
        var result = ByteArray(0)

        var count = read(buffer)
        while (count != -1) {
            val currentSize = result.size
            val copy = result.copyOf(currentSize + count)
            result = buffer.copyInto(copy, currentSize, count)
        }

        return result
    }

    fun toInputStream(): InputStream {
        return BlockingInputStream(this)
    }
}
