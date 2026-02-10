package com.patson

/* This modules is used for determining chronological ratio:
 * How many IRL days it takes to elapse 1 year of chronological progression?
 * How many Cycles it takes to elapse 1 year / month of chronological progression?   
 */
object ChronologyConverter {
  val WORLD_START_YEAR: Int = 1955
  val WORLD_END_YEAR: Int = 2030
  val PROGRESSION_REFERENCE_YEAR: Int = 1958 // 1958, not 1955! Need an offset, otherwise all planes release 3 years early!
  val YEAR_LENGTH_DAYS: Int = 14 // IRL Days!
  val DAY_LENGTH_MINUTES: Int = 24 * 60 // IRL Day expressed in minutes

  // Imported from MainSimulation!
  // Only represents minimum possible duration!
  val CYCLE_DURATION: Int = 45 * 60

  // Calculate cycles per chronological year using integer arithmetic
  private val minutesPerCycle: Int = CYCLE_DURATION / 60
  private val cyclesPerDay: Int = DAY_LENGTH_MINUTES / minutesPerCycle
  val cyclesPerYear: Int = cyclesPerDay * YEAR_LENGTH_DAYS

  // Calculate cycles per chronological month using integer arithmetic
  val cyclesPerChronologicalMonth: Int = cyclesPerYear / 12
}