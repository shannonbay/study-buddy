package com.github.shannonbay.studybuddy

import kotlinx.coroutines.*
import kotlin.coroutines.*

class Debouncer {
    private val debounceTimers = mutableMapOf<Any, Job>()

    fun shouldDebounce(key: Any, delayMillis: Long): Boolean {
        val existingJob = debounceTimers[key]
        if (existingJob == null || existingJob.isCompleted) {
            // No existing debounce timer or it has completed; do not debounce
            return false
        }
        return true
    }

    fun debounce(key: Any, delayMillis: Long, action: suspend () -> Unit) {
        val existingJob = debounceTimers[key]

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
