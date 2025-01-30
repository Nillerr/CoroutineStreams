package com.cardlay.app.micronaut

import io.micronaut.core.io.buffer.ByteBuffer

class DownloadedAWSS3File(
    val name: String,
    val content: ByteBuffer<*>,
)
