package com.cardlay.app.micronaut

import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.LinkedBlockingQueue

class BlockingTaskQueue<E : Any>(val parallelism: Int) {
    private val executor = Executors.newFixedThreadPool(parallelism)

    private val results = LinkedBlockingQueue<E>()

    fun submit(task: () -> E): Future<E> {
        return executor.submit(Callable {
            task().also { results.add(it) }
        })
    }

    fun poll(): E? {
        return results.poll()
    }
}
