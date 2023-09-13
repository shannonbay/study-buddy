package com.github.shannonbay.studybuddy

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class Debouncer {

    fun shouldDebounce(key: Any, delayMillis: Long): Boolean {
        val existingJob = debounceTimers[key]
        if (existingJob == null || existingJob.isCompleted) {
            // No existing debounce timer or it has completed; do not debounce
            return false
        }
        return true
    }

    fun debounceAfter(key: Any, delayMillis: Long, action: suspend () -> Unit) {
        val existingJob = debounceTimers[key]

        if (existingJob == null || existingJob.isCompleted) {
            // Create a new debounce timer if it doesn't exist or has completed
            val newJob = GlobalScope.launch {
                action() // Execute the function after the delay
                delay(delayMillis) // Delay for the specified period
            }

            // Store the new debounce timer associated with the key
            debounceTimers[key] = newJob

            // Set an onCompletion handler to remove the timer when it completes
            newJob.invokeOnCompletion { cause ->
                if (cause == null) {
                    debounceTimers.remove(key, newJob)
                }
            }
        }
    }


    private val debounceTimers = mutableMapOf<Any, Job>()
    fun throttleLast(key: Any, delayMillis: Long, action: suspend () -> Unit) {
        val existingJob = debounceTimers[key]
        existingJob?.cancel(null)

        // Create a new debounce timer if it doesn't exist or has completed
        val newJob = GlobalScope.launch {
            delay(delayMillis) // Delay for the specified period
            action() // Execute the function after the delay
        }

        // Store the new debounce timer associated with the key
        debounceTimers[key] = newJob

        // Set an onCompletion handler to remove the timer when it completes
        newJob.invokeOnCompletion { cause ->
            if (cause == null) {
                debounceTimers.remove(key, newJob)
            }
        }
    }

    fun cancel(key: Any) {
        val existingJob = debounceTimers[key]
        existingJob?.cancel(null)
    }

    fun debounceUntil(key: Any, delayMillis: Long, action: suspend () -> Unit) {
        val existingJob = debounceTimers[key]
        existingJob?.cancel(null)

        if (existingJob == null || existingJob.isCompleted) {
            // Create a new debounce timer if it doesn't exist or has completed
            val newJob = GlobalScope.launch {
                delay(delayMillis) // Delay for the specified period
                action() // Execute the function after the delay
            }

            // Store the new debounce timer associated with the key
            debounceTimers[key] = newJob

            // Set an onCompletion handler to remove the timer when it completes
            newJob.invokeOnCompletion { cause ->
                if (cause == null) {
                    debounceTimers.remove(key, newJob)
                }
            }
        }
    }
}
