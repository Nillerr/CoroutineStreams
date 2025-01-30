package com.cardlay.app.micronaut

interface AWSS3Client {
    fun getFile(id: String): AWSS3File
}
