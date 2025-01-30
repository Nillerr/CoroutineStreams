package com.cardlay.app.misc

import com.cardlay.app.SuspendingInputStream
import io.micronaut.core.io.buffer.ByteBufferFactory
import io.micronaut.core.type.Argument
import io.micronaut.core.type.MutableHeaders
import io.micronaut.http.ByteBodyHttpResponse
import io.micronaut.http.ByteBodyHttpResponseWrapper
import io.micronaut.http.HttpRequest
import io.micronaut.http.MediaType
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.body.ResponseBodyWriter
import io.micronaut.http.body.stream.InputStreamByteBody
import java.io.OutputStream

class SuspendingInputStreamWriter : ResponseBodyWriter<SuspendingInputStream> {
    override fun write(
        bufferFactory: ByteBufferFactory<*, *>,
        request: HttpRequest<*>,
        httpResponse: MutableHttpResponse<SuspendingInputStream>,
        type: Argument<SuspendingInputStream>,
        mediaType: MediaType,
        `object`: SuspendingInputStream,
    ): ByteBodyHttpResponse<*> {
        TODO("Not yet implemented")
    }

    override fun writeTo(
        type: Argument<SuspendingInputStream>,
        mediaType: MediaType,
        `object`: SuspendingInputStream,
        outgoingHeaders: MutableHeaders,
        outputStream: OutputStream,
    ) {
        TODO("Not yet implemented")
    }
}
