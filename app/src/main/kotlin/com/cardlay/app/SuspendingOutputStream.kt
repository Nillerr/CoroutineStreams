package com.cardlay.app

interface SuspendingOutputStream : SuspendingCloseable {
    suspend fun write(source: ByteArray) {
        write(source, 0, source.size)
    }

    suspend fun write(source: ByteArray, offset: Int, length: Int)
}
