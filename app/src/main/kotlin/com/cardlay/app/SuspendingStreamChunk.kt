package com.cardlay.app

internal class SuspendingStreamChunk(initialSize: Int) {
    var buffer = ByteArray(initialSize)

    var position = 0
    var size = -1

    val remaining: Int
        get() = size - position

    val isWrittenCompletely: Boolean
        get() = position == size

    fun ensureCapacity(minCapacity: Int) {
        if (buffer.size < minCapacity) {
            buffer = ByteArray(minCapacity)
        }
    }

    fun clear() {
        position = 0
        size = -1
    }

    companion object {
        val END_OF_STREAM = SuspendingStreamChunk(0)
    }
}
