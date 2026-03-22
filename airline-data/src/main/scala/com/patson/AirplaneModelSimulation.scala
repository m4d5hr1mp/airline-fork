package com.patson

import com.patson.data.airplane.ModelSource
import com.patson.model.airplane.Airplane

object AirplaneModelSimulation {

  def simulate(cycle: Int): Unit = {
    println("starting airplane model simulation")
    // All blanket model discounts (LOW_DEMAND) are removed.
    // Obsolescence discounts are computed at query time — no DB storage needed.
    // Purge any stale rows left over from previous system.
    ModelSource.updateModelDiscounts(List.empty)
    println("finished airplane model simulation")
  }
}