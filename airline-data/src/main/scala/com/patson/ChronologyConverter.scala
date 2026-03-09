package com.patson

/* This module determines chronological progression pacing.
 * Core unit: chronological weeks (52 per year).
 * cyclesPerChronologicalWeek controls unlock speed — tune for playtesting.
 * At 3: ~4.88 IRL days per in-game year, full 1955–2030 span in ~365 IRL days.
 */
object ChronologyConverter {
  val WORLD_START_YEAR: Int = 1955
  val WORLD_END_YEAR:   Int = 2030

  val WEEKS_PER_YEAR: Int = 52

  /** Game cycles per 1 chronological week. Main pacing knob. */
  val cyclesPerChronologicalWeek: Int = 3

  val cyclesPerYear: Int = cyclesPerChronologicalWeek * WEEKS_PER_YEAR  // 156
}