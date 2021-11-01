package com.padds.example.metrics

trait MetricTiming {
  def time[R](reporting: Double => Unit, block: => R): R = {
    val t0 = System.nanoTime()
    val result = block // call-by-name
    val t1 = System.nanoTime()
    val elapsedNano = t1 - t0
    val elapsedSeconds = elapsedNano / 1000000000.0
    reporting(elapsedSeconds)
    result
  }
}
