package com.cardlay.app

interface SuspendingCloseable {
    suspend fun close()
}

suspend inline fun <T : SuspendingCloseable> T.use(block: (T) -> Unit) {
    try {
        block(this)
    } finally {
        close()
    }
}
