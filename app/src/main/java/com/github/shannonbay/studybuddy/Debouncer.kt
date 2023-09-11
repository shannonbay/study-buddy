package com.github.shannonbay.studybuddy

import android.os.Build
import android.os.Handler
import androidx.annotation.RequiresApi
import kotlinx.coroutines.*
import kotlin.coroutines.*

@RequiresApi(Build.VERSION_CODES.P)
fun Handler.debounceDelayed(key: Any, delayMillis: Long, runnable: java.lang.Runnable) {
    // Remove any pending runnables associated with the given key
    removeCallbacksAndMessages(key)

    // Post the new runnable with the specified key and delay
    postDelayed(runnable, key, delayMillis)
}

@RequiresApi(Build.VERSION_CODES.P)
fun Handler.debounceUntil(key: Any, timeoutMillis: Long, action: () -> Unit) {
    // Execute the action immediately
    action()

    // Remove any pending callbacks
    removeCallbacksAndMessages(key)

    // Post a delayed callback to reset the debounce after the specified timeout
    postDelayed({ /* No action needed here */ }, key, timeoutMillis)
}
