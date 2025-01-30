package com.cardlay.app.micronaut

import java.io.InputStream

data class AWSS3File(
    val name: String,
    val size: Int,
    val inputStream: InputStream,
)
