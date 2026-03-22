package com.patson.data.airplane

import com.patson.data.Constants._
import com.patson.data.Meta
import com.patson.model.airplane.ModelOrderQueue

import scala.collection.mutable.ListBuffer
import scala.util.Random

object OrderQueueSource {

  // ── Read ──────────────────────────────────────────────────────────────────
  /**
   * Load all pending queue rows, sorted by (order_cycle ASC, shuffle_index ASC).
   * Simulation filters by model constructionTime on the Scala side.
   */
  def loadAllPendingRows(): List[ModelOrderQueue] = {
    val result = ListBuffer[ModelOrderQueue]()
    val connection = Meta.getConnection()
    try {
      val stmt = connection.prepareStatement(
        s"SELECT id, model_id, airline_id, order_cycle, shuffle_index, home_airport_id FROM $MODEL_ORDER_QUEUE_TABLE ORDER BY order_cycle ASC, shuffle_index ASC"
      )
      val rs = stmt.executeQuery()
      while (rs.next()) {
        result.append(ModelOrderQueue(
          modelId       = rs.getInt("model_id"),
          airlineId     = rs.getInt("airline_id"),
          orderCycle    = rs.getInt("order_cycle"),
          shuffleIndex  = rs.getInt("shuffle_index"),
          homeAirportId = rs.getInt("home_airport_id"),
          id            = rs.getInt("id")
        ))
      }
      rs.close()
      stmt.close()
    } finally {
      connection.close()
    }
    result.toList
  }

  def loadPendingOrdersByAirline(airlineId: Int): List[ModelOrderQueue] = {
    val result = ListBuffer[ModelOrderQueue]()
    val connection = Meta.getConnection()
    try {
      val stmt = connection.prepareStatement(
        s"""SELECT id, model_id, airline_id, order_cycle, shuffle_index, home_airport_id
            FROM $MODEL_ORDER_QUEUE_TABLE
            WHERE airline_id = ?
            ORDER BY order_cycle ASC, shuffle_index ASC"""
      )
      stmt.setInt(1, airlineId)
      val rs = stmt.executeQuery()
      while (rs.next()) {
        result.append(ModelOrderQueue(
          modelId       = rs.getInt("model_id"),
          airlineId     = rs.getInt("airline_id"),
          orderCycle    = rs.getInt("order_cycle"),
          shuffleIndex  = rs.getInt("shuffle_index"),
          homeAirportId = rs.getInt("home_airport_id"),
          id            = rs.getInt("id")
        ))
      }
      rs.close()
      stmt.close()
    } finally {
      connection.close()
    }
    result.toList
  }

  /**
   * Count of all pending rows for a given model (used for queue depth display).
   */
  def countPendingByModel(modelId: Int): Int = {
    var count = 0
    val connection = Meta.getConnection()
    try {
      val stmt = connection.prepareStatement(
        s"SELECT COUNT(*) FROM $MODEL_ORDER_QUEUE_TABLE WHERE model_id = ?"
      )
      stmt.setInt(1, modelId)
      val rs = stmt.executeQuery()
      if (rs.next()) count = rs.getInt(1)
      rs.close()
      stmt.close()
    } finally {
      connection.close()
    }
    count
  }

    /**
   * Returns (totalOrders, airlineOrders) for a given model.
   * Single query to avoid two round-trips.
   */
  def countPendingByModelAndAirline(modelId: Int, airlineId: Int): (Int, Int) = {
    var total = 0
    var airline = 0
    val connection = Meta.getConnection()
    try {
      val stmt = connection.prepareStatement(
        s"""SELECT
              COUNT(*) AS total,
              SUM(CASE WHEN airline_id = ? THEN 1 ELSE 0 END) AS airline_count
           FROM $MODEL_ORDER_QUEUE_TABLE
           WHERE model_id = ?"""
      )
      stmt.setInt(1, airlineId)
      stmt.setInt(2, modelId)
      val rs = stmt.executeQuery()
      if (rs.next()) {
        total   = rs.getInt("total")
        airline = rs.getInt("airline_count")
      }
      rs.close()
      stmt.close()
    } finally {
      connection.close()
    }
    (total, airline)
  }

  // ── Write ─────────────────────────────────────────────────────────────────
  /**
   * Insert `quantity` new rows for (modelId, airlineId, orderCycle, homeAirportId),
   * then reshuffle ALL rows sharing (modelId, orderCycle) by reassigning random
   * shuffle_index values. Done in a single transaction.
   *
   * MySQL 5.7 InnoDB: SELECT ... FOR UPDATE works inside explicit transactions.
   */
  def insertAndReshuffle(modelId: Int, airlineId: Int, quantity: Int, orderCycle: Int, homeAirportId: Int): Unit = {
    val connection = Meta.getConnection()
    try {
      connection.setAutoCommit(false)
      try {
        // 1. Insert new rows with placeholder shuffle_index = 0
        val insertStmt = connection.prepareStatement(
          s"INSERT INTO $MODEL_ORDER_QUEUE_TABLE (model_id, airline_id, order_cycle, shuffle_index, home_airport_id) VALUES (?, ?, ?, 0, ?)"
        )
        for (_ <- 0 until quantity) {
          insertStmt.setInt(1, modelId)
          insertStmt.setInt(2, airlineId)
          insertStmt.setInt(3, orderCycle)
          insertStmt.setInt(4, homeAirportId)
          insertStmt.addBatch()
        }
        insertStmt.executeBatch()
        insertStmt.close()

        // 2. Lock and load all row IDs for this (modelId, orderCycle) batch
        val selectStmt = connection.prepareStatement(
          s"SELECT id FROM $MODEL_ORDER_QUEUE_TABLE WHERE model_id = ? AND order_cycle = ? FOR UPDATE"
        )
        selectStmt.setInt(1, modelId)
        selectStmt.setInt(2, orderCycle)
        val rs = selectStmt.executeQuery()
        val ids = ListBuffer[Int]()
        while (rs.next()) ids.append(rs.getInt("id"))
        rs.close()
        selectStmt.close()

        // 3. Shuffle IDs and reassign shuffle_index
        val shuffled = Random.shuffle(ids.toList)
        val updateStmt = connection.prepareStatement(
          s"UPDATE $MODEL_ORDER_QUEUE_TABLE SET shuffle_index = ? WHERE id = ?"
        )
        shuffled.zipWithIndex.foreach { case (rowId, idx) =>
          updateStmt.setInt(1, idx)
          updateStmt.setInt(2, rowId)
          updateStmt.addBatch()
        }
        updateStmt.executeBatch()
        updateStmt.close()

        connection.commit()
      } catch {
        case e: Exception =>
          connection.rollback()
          throw e
      } finally {
        connection.setAutoCommit(true)
      }
    } finally {
      connection.close()
    }
  }

  // ── Delete ────────────────────────────────────────────────────────────────
  /**
   * Delete delivered rows by primary key.
   */
  def deleteRows(ids: List[Int]): Unit = {
    if (ids.isEmpty) return
    val connection = Meta.getConnection()
    try {
      val placeholders = ids.map(_ => "?").mkString(",")
      val stmt = connection.prepareStatement(
        s"DELETE FROM $MODEL_ORDER_QUEUE_TABLE WHERE id IN ($placeholders)"
      )
      ids.zipWithIndex.foreach { case (id, idx) => stmt.setInt(idx + 1, id) }
      stmt.executeUpdate()
      stmt.close()
    } finally {
      connection.close()
    }
  }
}