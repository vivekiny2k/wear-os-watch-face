package com.watchapp.data

import java.util.concurrent.atomic.AtomicReference

object FlightLocationStore {
  private val coords = AtomicReference(39.3601 to -84.3099)

  fun update(lat: Double, lon: Double) {
    coords.set(lat to lon)
  }

  fun get(): Pair<Double, Double> = coords.get()
}
