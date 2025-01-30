package com.cardlay.app.misc

import com.cardlay.app.SuspendingInputStream
import io.micronaut.core.execution.ExecutionFlow
import io.micronaut.core.io.buffer.ByteBuffer
import io.micronaut.core.io.buffer.ByteBufferFactory
import io.micronaut.http.body.ByteBody
import io.micronaut.http.body.CloseableAvailableByteBody
import io.micronaut.http.body.CloseableByteBody
import io.micronaut.http.body.InternalByteBody
import io.micronaut.http.body.stream.AvailableByteArrayBody
import io.micronaut.scheduling.TaskExecutors
import jakarta.inject.Named
import kotlinx.coroutines.reactive.publish
import org.reactivestreams.Publisher
import java.io.InputStream
import java.util.OptionalLong
import java.util.concurrent.ExecutorService

class SuspendingInputStreamByteBody(
    private val stream: SuspendingInputStream,
    private val byteBufferFactory: ByteBufferFactory<*, *>,
    @Named(TaskExecutors.BLOCKING) private val ioExecutor: ExecutorService,
) : ByteBody, InternalByteBody {
    override fun split(backpressureMode: ByteBody.SplitBackpressureMode): CloseableByteBody {
        TODO("Not yet implemented")
    }

    override fun expectedLength(): OptionalLong {
        return OptionalLong.empty()
    }

    override fun toInputStream(): InputStream {
        return stream.toInputStream()
    }

    override fun toByteArrayPublisher(): Publisher<ByteArray> {
        return publish {
            var buffer = ByteArray(8192)

            var read = stream.read(buffer)
            while (read != -1) {
                val bytes = buffer.copyOf(read)
                send(bytes)

                read = stream.read(buffer)
            }
        }
    }

    override fun toByteBufferPublisher(): Publisher<ByteBuffer<*>> {
        return publish {
            var buffer = ByteArray(8192)

            var read = stream.read(buffer)
            while (read != -1) {
                val bytes = buffer.copyOf(read)
                val byteBuffer = byteBufferFactory.wrap(bytes)
                send(byteBuffer)

                read = stream.read(buffer)
            }
        }
    }

    override fun bufferFlow(): ExecutionFlow<out CloseableAvailableByteBody> {
        return ExecutionFlow.async(ioExecutor) {
            val stream = stream.toInputStream()
            try {
                ExecutionFlow.just(AvailableByteArrayBody.create(byteBufferFactory, stream.readAllBytes()))
            } catch (e: Exception) {
                ExecutionFlow.error(e)
            } finally {
                stream.close()
            }
        }
    }
}
