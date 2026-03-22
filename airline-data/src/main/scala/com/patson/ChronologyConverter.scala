package com.patson

/* This module determines chronological progression pacing.
 * Core unit: chronological weeks (52 per year).
 * cyclesPerChronologicalWeek controls unlock speed — tune for playtesting.
 * At 3: ~4.88 IRL days per in-game year, full 1955–2030 span in ~365 IRL days.
 */
object ChronologyConverter {
  val WORLD_START_YEAR: Int = 1955
<<<<<<< HEAD
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
=======
  val WORLD_END_YEAR:   Int = 2030

  val WEEKS_PER_YEAR: Int = 52

  /** Game cycles per 1 chronological week. Main pacing knob. */
  val cyclesPerChronologicalWeek: Int = 3

  val cyclesPerYear: Int = cyclesPerChronologicalWeek * WEEKS_PER_YEAR  // 156
>>>>>>> b3687362a7034d069cb08f3dd335716a6503b9d4
}