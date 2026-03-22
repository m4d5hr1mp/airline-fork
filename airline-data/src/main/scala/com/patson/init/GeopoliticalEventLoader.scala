package com.patson.init

import com.patson.data.GeopoliticalEventSource
import com.patson.model.{EventAction, GeopoliticalEvent}

/**
 * One-shot CSV loader that seeds the geopolitical_event table from a semicolon-delimited file.
 *
 * Expected format (semicolon-delimited, no quoting needed):
 *   year;week;origin;action;target;value
 *
 * - value column may be empty for actions that don't require it (JOIN_BLOCK, LEAVE_BLOCK etc.)
 * - Lines starting with # are treated as comments and skipped
 * - Header line (year;week;...) is skipped automatically
 *
 * Designed to be called once during MainInit before WorldStateGenerator.runInit().
 * Safe to call on a fresh schema — geopolitical_event table must already exist.
 */
object GeopoliticalEventLoader {

  def loadFromCsv(path: String): Unit = {
    println(s"[GeopoliticalEventLoader] loading from $path")

    val lines = scala.io.Source.fromFile(path).getLines()
      .map(_.trim)
      .filterNot(l => l.isEmpty || l.startsWith("#"))
      .toList

    val dataLines = if (lines.headOption.exists(_.startsWith("year"))) lines.tail else lines

    val events = dataLines.zipWithIndex.flatMap { case (line, idx) =>
      val parts = line.split(";", -1)
      if (parts.length < 5) {
        println(s"  WARN: line ${idx + 2} has fewer than 5 fields, skipping: $line")
        None
      } else {
        val yearStr   = parts(0).trim
        val weekStr   = parts(1).trim
        val origin    = parts(2).trim
        val actionStr = parts(3).trim
        val target    = parts(4).trim
        val valueStr  = if (parts.length > 5) parts(5).trim else ""

        val yearOpt   = yearStr.toIntOption
        val weekOpt   = weekStr.toIntOption
        val actionOpt = EventAction.fromString(actionStr)
        val valueOpt  = if (valueStr.isEmpty) None else valueStr.toIntOption.orElse {
          println(s"  WARN: line ${idx + 2} has non-integer value '$valueStr', treating as empty")
          None
        }

        (yearOpt, weekOpt, actionOpt) match {
          case (Some(year), Some(week), Some(action)) =>
            Some(GeopoliticalEvent(
              id             = 0,
              year           = year,
              week           = week,
              executionCycle = 0,  // not used for storage — computed at load time
              origin         = origin,
              action         = action,
              target         = target,
              value          = valueOpt
            ))
          case (None, _, _) =>
            println(s"  WARN: line ${idx + 2} invalid year '$yearStr', skipping")
            None
          case (_, None, _) =>
            println(s"  WARN: line ${idx + 2} invalid week '$weekStr', skipping")
            None
          case (_, _, None) =>
            println(s"  WARN: line ${idx + 2} unknown action '$actionStr', skipping")
            None
        }
      }
    }

    println(s"  parsed ${events.size} events from ${dataLines.size} lines")
    GeopoliticalEventSource.saveEvents(events)
    println(s"[GeopoliticalEventLoader] DONE")
  }
}