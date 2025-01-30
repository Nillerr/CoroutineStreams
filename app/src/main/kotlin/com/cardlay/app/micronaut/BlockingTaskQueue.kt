package com.cardlay.app.micronaut

import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class BlockingTaskQueue<E : Any>(
    val parallelism: Int,
    val keepAliveTime: Long = 5L,
    val unit: TimeUnit = TimeUnit.SECONDS,
) : AutoCloseable {
    private val eos = Any()

    private val executor = ThreadPoolExecutor(parallelism, parallelism, keepAliveTime, unit, LinkedBlockingQueue())
    private val results = LinkedBlockingQueue<Any>()

    fun submit(task: () -> E): Future<E> {
        return executor.submit(Callable {
            task().also { results.add(it) }
        })
    }

    @Suppress("UNCHECKED_CAST")
    fun dequeue(): E? {
        val result = results.take()
        if (result == eos) {
            return null
        }

        return result as E
    }

    override fun close() {
        executor.shutdown()
        results.add(eos)
    }
}
