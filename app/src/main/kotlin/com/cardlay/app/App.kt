package com.cardlay.app

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

suspend fun main() {
    SuspendingQueueOutputStream(queueSize = 2, initialChunkSize = 1024).use { outputStream ->
        SuspendingQueueInputStream(outputStream).use { inputStream ->
            coroutineScope {
                launch {
                    val bytes = mutableListOf<ByteArray>(
                        byteArrayOf(1, 2, 3, 4, 5),
                        byteArrayOf(6, 7, 8, 9, 10),
                        byteArrayOf(11, 12, 13, 14, 15),
                        byteArrayOf(16, 17, 18, 19, 20),
                    )

                    while (bytes.isNotEmpty()) {
                        val buffer = bytes.removeFirst()
                        outputStream.write(buffer)
                        println("[${Thread.currentThread().id}] Wrote: ${buffer.joinToString()}")
                    }

                    outputStream.close()
                }

                launch {
                    val buffer = ByteArray(5)
                    while (inputStream.read(buffer) != -1) {
                        println("[${Thread.currentThread().id}] Read: ${buffer.joinToString()}")
                    }

                    inputStream.close()
                }
            }
        }
    }
}
