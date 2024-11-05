package com.recyclerlist.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class Debounce(private val coroutineScope: CoroutineScope) {
    private var debounceJob: Job? = null

    fun withDelay(delayMillis: Long, action: () -> Unit) {
        debounceJob?.cancel()
        debounceJob = coroutineScope.launch {
            delay(delayMillis)
            action()
        }
    }
}
