package com.cardlay.app

import kotlinx.coroutines.runBlocking
import java.io.InputStream

class BlockingInputStream(val stream: SuspendingInputStream) : InputStream() {
    override fun read(): Int {
        return runBlocking { stream.read() }
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        return runBlocking { stream.read(b, off, len) }
    }

    override fun readAllBytes(): ByteArray? {
        return runBlocking { stream.readAllBytes() }
    }

    override fun close() {
        return runBlocking { stream.close() }
    }
}
