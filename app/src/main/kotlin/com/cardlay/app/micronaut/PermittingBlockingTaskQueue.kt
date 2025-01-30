package com.cardlay.app.micronaut

import java.util.concurrent.Future
import java.util.concurrent.Semaphore

class PermittingBlockingTaskQueue<E : Any>(val queue: BlockingTaskQueue<E>) {
    private val semaphore = Semaphore(queue.parallelism)

    fun submit(task: () -> E): Future<E> {
        semaphore.acquire()

        return queue.submit {
            try {
                task()
            } finally {
                semaphore.release()
            }
        }
    }
}

fun <E : Any> BlockingTaskQueue<E>.asPermittingBlockingTaskQueue(): PermittingBlockingTaskQueue<E> {
    return PermittingBlockingTaskQueue(this)
}
