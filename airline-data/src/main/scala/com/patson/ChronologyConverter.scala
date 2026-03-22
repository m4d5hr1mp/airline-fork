package com.patson

/* This modules is used for determining chronological ratio:
 * How many IRL days it takes to elapse 1 year of chronological progression?
 * How many Cycles it takes to elapse 1 year / month of chronological progression?   
 */
object ChronologyConverter {
  val WORLD_START_YEAR: Int = 1955
  val WORLD_END_YEAR: Int = 2030
  val PROGRESSION_REFERENCE_YEAR: Int = 1958
  val YEAR_LENGTH_DAYS: Int = 5
  val DAY_LENGTH_MINUTES: Int = 24 * 60

  val CYCLE_DURATION: Int = 30 * 60

  private val minutesPerCycle: Int = CYCLE_DURATION / 60
  private val cyclesPerDay: Int = DAY_LENGTH_MINUTES / minutesPerCycle
  val cyclesPerYear: Int = cyclesPerDay * YEAR_LENGTH_DAYS   // 240 on test

  val cyclesPerChronologicalMonth: Int = cyclesPerYear / 12   // 20 on test

  // ====================== SIMPLE HELPERS ADDED FOR PROGRESSIVE UNLOCKS ======================
  /** Convert weeks since 01.01.1958 → exact game cycle (week-precise) */
  def weeksSince1958ToCycle(weeks: Int): Int = {
    if (weeks <= 0) 0
    else {
      val referenceCycle = (PROGRESSION_REFERENCE_YEAR - WORLD_START_YEAR) * cyclesPerYear  // 720
      referenceCycle + math.round(weeks.toDouble * cyclesPerYear / 52.0).toInt
    }
  }

  def cycleToGameYear(cycle: Int): Int =
    WORLD_START_YEAR + (cycle / cyclesPerYear)

  def cycleToGameDateString(cycle: Int): String = {
    val year = cycleToGameYear(cycle)
    val monthIndex = (cycle % cyclesPerYear) / cyclesPerChronologicalMonth
    val MONTH_NAMES = Vector("January","February","March","April","May","June","July","August","September","October","November","December")
    s"${MONTH_NAMES(monthIndex)} $year"
  }
}