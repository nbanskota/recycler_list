package com.recyclerlist.utils

import java.util.*

class IntervalRunner(private val intervalMillis: Long, private val task: () -> Unit) {

  private var timer: Timer? = null

  fun start() {
    timer = Timer()
    timer?.schedule(object : TimerTask() {
      override fun run() {
        task()
      }
    }, 0, intervalMillis)
  }

  fun stop() {
    timer?.cancel()
    timer = null
  }
}
