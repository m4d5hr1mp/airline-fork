package com.patson.data

import com.patson.data.Constants._
import com.patson.model.{CountryBlock, CountryBlockRelation}

object CountryBlockSource {

  // ── country_block read ────────────────────────────────────────────────────

  def loadAllBlocks(): List[CountryBlock] = {
    val connection = Meta.getConnection()
    try {
      val stmt = connection.prepareStatement(
        s"SELECT block_name, relation_value, members, enforce_relations FROM $COUNTRY_BLOCK_TABLE"
      )
      val rs  = stmt.executeQuery()
      val buf = scala.collection.mutable.ListBuffer[CountryBlock]()
      while (rs.next()) {
        buf += CountryBlock(
          blockName        = rs.getString("block_name"),
          relationValue    = rs.getInt("relation_value"),
          members          = CountryBlock.parseCsv(rs.getString("members")),
          enforceRelations = rs.getInt("enforce_relations") == 1
        )
      }
      rs.close(); stmt.close()
      buf.toList
    } finally { connection.close() }
  }

  def loadBlockByName(blockName: String): Option[CountryBlock] = {
    val connection = Meta.getConnection()
    try {
      val stmt = connection.prepareStatement(
        s"SELECT block_name, relation_value, members, enforce_relations FROM $COUNTRY_BLOCK_TABLE WHERE block_name = ?"
      )
      stmt.setString(1, blockName)
      val rs = stmt.executeQuery()
      val result = if (rs.next()) Some(CountryBlock(
        blockName        = rs.getString("block_name"),
        relationValue    = rs.getInt("relation_value"),
        members          = CountryBlock.parseCsv(rs.getString("members")),
        enforceRelations = rs.getInt("enforce_relations") == 1
      )) else None
      rs.close(); stmt.close()
      result
    } finally { connection.close() }
  }

  // ── country_block write ───────────────────────────────────────────────────

  def saveBlock(block: CountryBlock): Unit = saveBlocks(List(block))

  def saveBlocks(blocks: List[CountryBlock]): Unit = {
    val connection = Meta.getConnection()
    try {
      connection.setAutoCommit(false)
      val stmt = connection.prepareStatement(
        s"REPLACE INTO $COUNTRY_BLOCK_TABLE (block_name, relation_value, members, enforce_relations) VALUES (?, ?, ?, ?)"
      )
      blocks.foreach { b =>
        stmt.setString(1, b.blockName)
        stmt.setInt(2, b.relationValue)
        stmt.setString(3, b.membersAsCsv)
        stmt.setInt(4, if (b.enforceRelations) 1 else 0)
        stmt.addBatch()
      }
      stmt.executeBatch()
      connection.commit()
      stmt.close()
    } finally { connection.close() }
  }

  def addMember(blockName: String, countryCode: String): Unit =
    loadBlockByName(blockName).foreach { block =>
      if (!block.members.contains(countryCode))
        saveBlock(block.copy(members = block.members :+ countryCode))
    }

  def removeMember(blockName: String, countryCode: String): Unit =
    loadBlockByName(blockName).foreach { block =>
      saveBlock(block.copy(members = block.members.filterNot(_ == countryCode)))
    }

  def deleteBlock(blockName: String): Unit = {
    val connection = Meta.getConnection()
    try {
      // CASCADE will also delete country_block_relation rows for this block
      val stmt = connection.prepareStatement(s"DELETE FROM $COUNTRY_BLOCK_TABLE WHERE block_name = ?")
      stmt.setString(1, blockName)
      stmt.executeUpdate()
      stmt.close()
    } finally { connection.close() }
  }

  // ── country_block_relation read ───────────────────────────────────────────

  def loadBlockRelations(blockName: String): List[CountryBlockRelation] = {
    val connection = Meta.getConnection()
    try {
      val stmt = connection.prepareStatement(
        s"SELECT block_name, target, relation_value FROM $COUNTRY_BLOCK_RELATION_TABLE WHERE block_name = ?"
      )
      stmt.setString(1, blockName)
      val rs  = stmt.executeQuery()
      val buf = scala.collection.mutable.ListBuffer[CountryBlockRelation]()
      while (rs.next())
        buf += CountryBlockRelation(rs.getString("block_name"), rs.getString("target"), rs.getInt("relation_value"))
      rs.close(); stmt.close()
      buf.toList
    } finally { connection.close() }
  }

  def loadAllBlockRelations(): List[CountryBlockRelation] = {
    val connection = Meta.getConnection()
    try {
      val stmt = connection.prepareStatement(
        s"SELECT block_name, target, relation_value FROM $COUNTRY_BLOCK_RELATION_TABLE"
      )
      val rs  = stmt.executeQuery()
      val buf = scala.collection.mutable.ListBuffer[CountryBlockRelation]()
      while (rs.next())
        buf += CountryBlockRelation(rs.getString("block_name"), rs.getString("target"), rs.getInt("relation_value"))
      rs.close(); stmt.close()
      buf.toList
    } finally { connection.close() }
  }

  // ── country_block_relation write ──────────────────────────────────────────

  def saveBlockRelation(rel: CountryBlockRelation): Unit = {
    val connection = Meta.getConnection()
    try {
      val stmt = connection.prepareStatement(
        s"REPLACE INTO $COUNTRY_BLOCK_RELATION_TABLE (block_name, target, relation_value) VALUES (?, ?, ?)"
      )
      stmt.setString(1, rel.blockName)
      stmt.setString(2, rel.target)
      stmt.setInt(3, rel.relationValue)
      stmt.executeUpdate()
      stmt.close()
    } finally { connection.close() }
  }

  def deleteBlockRelation(blockName: String, target: String): Unit = {
    val connection = Meta.getConnection()
    try {
      val stmt = connection.prepareStatement(
        s"DELETE FROM $COUNTRY_BLOCK_RELATION_TABLE WHERE block_name = ? AND target = ?"
      )
      stmt.setString(1, blockName)
      stmt.setString(2, target)
      stmt.executeUpdate()
      stmt.close()
    } finally { connection.close() }
  }
}