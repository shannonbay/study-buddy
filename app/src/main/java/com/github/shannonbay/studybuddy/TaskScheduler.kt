package com.github.shannonbay.studybuddy

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

object TaskScheduler {
    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    private val futures = mutableMapOf<Any, ScheduledFuture<*>>()
    fun reschedule(delayMillis: Long, runnable: Runnable): ScheduledFuture<*> {
        val existingTask = futures[runnable]
        existingTask?.cancel(false)
        val future = executor.schedule(runnable, delayMillis, TimeUnit.MILLISECONDS)
        futures[runnable] = future
        return future
    }
}
