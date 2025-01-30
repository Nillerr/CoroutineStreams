package com.cardlay.app

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import java.io.IOException
import java.util.*
import kotlin.math.min

class SuspendingQueueInputStream(val queueSize: Int = 4) : SuspendingCloseable {
    constructor(source: SuspendingQueueOutputStream) : this(source.queueSize) {
        connect(source)
    }

    var closed: Boolean = false
        private set

    internal var source: SuspendingQueueOutputStream? = null

    val suspendingQueueOutputStream: SuspendingQueueOutputStream
        get() = source ?: throw IOException("The SuspendingInputStream is not connected to a SuspendingOutputStream.")

    internal val channel = Channel<SuspendingStreamChunk>(queueSize, BufferOverflow.SUSPEND)

    private var reading: SuspendingStreamChunk? = null

    fun connect(source: SuspendingQueueOutputStream) {
        if (this.source != null) {
            throw IOException("The SuspendingInputStream is already connected to a SuspendingOutputStream.")
        }

        this.source = source
        source.sink = this
    }

    suspend fun read(destination: ByteArray): Int {
        return read(destination, 0, destination.size)
    }

    suspend fun read(destination: ByteArray, offset: Int, length: Int): Int {
        suspendingQueueOutputStream.ensureInitialized()

        Objects.checkFromIndexSize(offset, destination.size, length)
        if (length == 0) {
            return 0
        }

        val chunk = reading ?: channel.receive()
            .also { reading = it }

        if (chunk == SuspendingStreamChunk.END_OF_STREAM) {
            return -1
        }

        return readChunk(chunk, destination, offset, length)
    }

    private suspend fun readChunk(chunk: SuspendingStreamChunk, destination: ByteArray, offset: Int, length: Int): Int {
        val remaining = chunk.remaining
        val count = min(length, remaining)
        val startIndex = chunk.position
        val endIndex = chunk.position + count
        chunk.buffer.copyInto(destination, offset, startIndex, endIndex)
        chunk.position += count

        if (chunk.isWrittenCompletely) {
            reading = null
            chunk.clear()
            sendChunkToOutputStream(chunk)
        }

        return count
    }

    private suspend fun sendChunkToOutputStream(chunk: SuspendingStreamChunk) {
        if (!suspendingQueueOutputStream.closed) {
            try {
                suspendingQueueOutputStream.channel.send(chunk)
            } catch (e: ClosedSendChannelException) {
                if (!suspendingQueueOutputStream.closed) {
                    throw e
                }
            }
        }
    }

    override suspend fun close() {
        if (closed) {
            return
        }

        closed = true
        channel.close()
    }
}
