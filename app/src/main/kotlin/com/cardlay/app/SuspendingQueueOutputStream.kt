package com.cardlay.app

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.util.*

class SuspendingQueueOutputStream(
    val queueSize: Int = 4,
    private val initialChunkSize: Int = 1024,
) : SuspendingCloseable {
    constructor(
        sink: SuspendingQueueInputStream,
        initialChunkSize: Int = 1024,
    ) : this(sink.queueSize, initialChunkSize) {
        connect(sink)
    }

    var closed: Boolean = false
        private set

    internal var sink: SuspendingQueueInputStream? = null

    val suspendingQueueInputStream: SuspendingQueueInputStream
        get() = sink ?: throw IOException("The SuspendingOutputStream is not connected to a SuspendingInputStream.")

    internal val channel = Channel<SuspendingStreamChunk>(queueSize, BufferOverflow.SUSPEND)

    private val mutex = Mutex()

    private var initialized: Boolean = false

    fun connect(sink: SuspendingQueueInputStream) {
        if (this.sink != null) {
            throw IOException("The SuspendingOutputStream is already connected to a SuspendingInputStream.")
        }

        this.sink = sink
        sink.source = this
    }

    internal suspend fun ensureInitialized() {
        if (!initialized) {
            mutex.withLock {
                if (!initialized) {
                    initialize()
                    initialized = true
                }
            }
        }
    }

    private suspend fun initialize() {
        repeat(queueSize) {
            val chunk = SuspendingStreamChunk(initialChunkSize)
            channel.send(chunk)
        }
    }

    suspend fun write(source: ByteArray) {
        write(source, 0, source.size)
    }

    suspend fun write(source: ByteArray, offset: Int, length: Int) {
        ensureInitialized()
        Objects.checkFromIndexSize(offset, source.size, length)

        val chunk = channel.receive()
        chunk.ensureCapacity(length)

        source.copyInto(chunk.buffer, 0, offset, length)
        chunk.size = length

        suspendingQueueInputStream.channel.send(chunk)
    }

    override suspend fun close() {
        if (closed) {
            return
        }

        closed = true
        suspendingQueueInputStream.channel.send(SuspendingStreamChunk.END_OF_STREAM)
        channel.close()
    }
}
