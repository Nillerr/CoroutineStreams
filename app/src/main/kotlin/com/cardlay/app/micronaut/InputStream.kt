package com.cardlay.app.micronaut

import io.micronaut.core.io.buffer.ByteBuffer
import java.io.InputStream

fun InputStream.copyTo(out: ByteBuffer<*>, bufferSize: Int = DEFAULT_BUFFER_SIZE): Long {
    var bytesCopied: Long = 0
    val buffer = ByteArray(bufferSize)
    var bytes = read(buffer)
    while (bytes >= 0) {
        out.write(buffer, 0, bytes)
        bytesCopied += bytes
        bytes = read(buffer)
    }
    return bytesCopied
}
