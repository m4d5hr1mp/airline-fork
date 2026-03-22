package com.patson.data

import com.patson.ChronologyConverter
import com.patson.data.Constants._
import com.patson.model.{EventAction, GeopoliticalEvent}

import scala.collection.mutable.ListBuffer

object GeopoliticalEventSource {

  val INIT_CYCLE = -1

  // ── Read ──────────────────────────────────────────────────────────────────

  def loadAllEvents(): List[GeopoliticalEvent] =
    loadAllFromDb().sortBy(e => (e.executionCycle, e.id))

  /** Init events — year=-1, week=-1, executionCycle=-1. Fired once before game loop. */
  def loadInitEvents(): List[GeopoliticalEvent] =
    loadAllFromDb().filter(_.executionCycle == INIT_CYCLE).sortBy(_.id)

  /** All events that fire on a specific game cycle. */
  def loadEventsForCycle(cycle: Int): List[GeopoliticalEvent] =
    loadAllFromDb().filter(_.executionCycle == cycle).sortBy(_.id)

  // execution_cycle is NOT stored in DB — computed from (year, week) at load time.
  // Total row count is small (~600-800 rows), so full-table load + in-memory filter is fine.
  private def loadAllFromDb(): List[GeopoliticalEvent] = {
    val connection = Meta.getConnection()
    try {
      val stmt = connection.prepareStatement(
        s"SELECT id, year, week, origin, action, target, delta_value FROM $GEOPOLITICAL_EVENT_TABLE"
      )
      val rs  = stmt.executeQuery()
      val buf = ListBuffer[GeopoliticalEvent]()
      while (rs.next()) {
        val year  = rs.getInt("year")
        val week  = rs.getInt("week")
        val value = { val v = rs.getInt("delta_value"); if (rs.wasNull()) None else Some(v) }
        EventAction.fromString(rs.getString("action")).foreach { action =>
          buf += GeopoliticalEvent(
            id             = rs.getInt("id"),
            year           = year,
            week           = week,
            executionCycle = toCycle(year, week),
            origin         = rs.getString("origin"),
            action         = action,
            target         = rs.getString("target"),
            value          = value
          )
        }
      }
      rs.close()
      stmt.close()
      buf.toList
    } finally {
      connection.close()
    }
  }

  // year=-1, week=-1 is the init sentinel — returns -1.
  // Otherwise: cycles elapsed since WORLD_START_YEAR week 1.
  private def toCycle(year: Int, week: Int): Int = {
    if (year == -1 && week == -1) return -1
    val yearOffset = year - ChronologyConverter.WORLD_START_YEAR
    val weekOffset = week - 1
    yearOffset * ChronologyConverter.cyclesPerYear +
      weekOffset * ChronologyConverter.cyclesPerChronologicalWeek
  }

  // ── Write ─────────────────────────────────────────────────────────────────

  def saveEvent(event: GeopoliticalEvent): Int = {
    val connection = Meta.getConnection()
    try {
      val stmt = connection.prepareStatement(
        s"INSERT INTO $GEOPOLITICAL_EVENT_TABLE (year, week, origin, action, target, delta_value) VALUES (?,?,?,?,?,?)",
        java.sql.Statement.RETURN_GENERATED_KEYS
      )
      bindEvent(stmt, event)
      stmt.executeUpdate()
      val keys = stmt.getGeneratedKeys
      val id   = if (keys.next()) keys.getInt(1) else -1
      keys.close(); stmt.close()
      id
    } finally {
      connection.close()
    }
  }

  def saveEvents(events: List[GeopoliticalEvent]): Unit = {
    val connection = Meta.getConnection()
    try {
      connection.setAutoCommit(false)
      val stmt = connection.prepareStatement(
        s"INSERT INTO $GEOPOLITICAL_EVENT_TABLE (year, week, origin, action, target, delta_value) VALUES (?,?,?,?,?,?)"
      )
      events.foreach { e => bindEvent(stmt, e); stmt.addBatch() }
      stmt.executeBatch()
      connection.commit()
      stmt.close()
    } finally {
      connection.close()
    }
  }

  private def bindEvent(stmt: java.sql.PreparedStatement, e: GeopoliticalEvent): Unit = {
    stmt.setInt(1, e.year)
    stmt.setInt(2, e.week)
    stmt.setString(3, e.origin)
    stmt.setString(4, e.action.entryName)
    stmt.setString(5, e.target)
    e.value match {
      case Some(v) => stmt.setInt(6, v)
      case None    => stmt.setNull(6, java.sql.Types.INTEGER)
    }
  }
}