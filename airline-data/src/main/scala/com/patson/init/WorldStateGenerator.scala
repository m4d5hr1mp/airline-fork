package com.patson.init

import com.patson.data.{CountryBlockSource, CountrySource, GeopoliticalEventSource}
import com.patson.model.{CountryBlock, CountryBlockRelation, EventAction, GeopoliticalEvent}

import scala.collection.mutable

/**
 * Computes and persists country mutual relations from geopolitical events.
 *
 * ALL event values are ABSOLUTE TARGETS, not deltas.
 *
 * Entry points:
 *   runInit()        — processes execution_cycle = -1; full purge + reinsert
 *   runForCycle(n)   — processes events for cycle n; early-exit if none; patches touched pairs only
 *
 * Processing order within a cycle:
 *
 *   BATCH 1 — Block mutations and enforcement (in event log order):
 *     CREATE_BLOCK, DISSOLVE_BLOCK, JOIN_BLOCK, LEAVE_BLOCK
 *     SET_BLOCK_VALUE     — sets intra-block relation_value to absolute target
 *     SET_BLOCK_ENFORCE   — enables/disables enforcement; propagates on enable
 *     SET_BLOCK_RELATIONS — updates country_block_relation; propagates if enforced
 *
 *   [recompute full block baseline from updated country_block state]
 *
 *   BATCH 2 — Direct relation overrides (in event log order):
 *     SET_RELATIONS           — country origins only; block-floor-respecting
 *     SET_RELATIONS_OVERRIDE  — any origin; unconditional; same power as enforcement
 *
 * Enforcement conflict resolution: max absolute value wins across enforcing blocks.
 */
object WorldStateGenerator {

  val RELATION_MIN = -5
  val RELATION_MAX =  5

  // Accumulates enforced values during batch 1; flushed at start of batch 2.
  // Key is always (lower, higher) alphabetically to avoid (A,B)/(B,A) duplicates.
  private val enforcementAccumulator = mutable.Map[(String, String), Int]()

  // ── Entry points ─────────────────────────────────────────────────────────────

  def runInit(): Unit = {
    println("[WorldStateGenerator] INIT — building full relation map")
    val events     = GeopoliticalEventSource.loadInitEvents()
    val validCodes = CountrySource.loadAllCountries().map(_.countryCode).toSet

    applyBlockEvents(events, validCodes)

    val blocks      = CountryBlockSource.loadAllBlocks()
    val relationMap = buildFullRelationMap(blocks, validCodes)
    applyRelationEvents(events, relationMap, blocks, validCodes)

    val safeMap = relationMap.filter { case ((c1, c2), _) => validCodes.contains(c1) && validCodes.contains(c2) }
    println(s"  persisting ${safeMap.size} pairs (full rewrite)")
    CountrySource.updateCountryMutualRelationships(safeMap)
    println("[WorldStateGenerator] INIT DONE")
  }

  def runForCycle(cycle: Int): Unit = {
    val events = GeopoliticalEventSource.loadEventsForCycle(cycle)
    if (events.isEmpty) return

    println(s"[WorldStateGenerator] cycle $cycle — ${events.size} events")
    val validCodes = CountrySource.loadAllCountries().map(_.countryCode).toSet

    applyBlockEvents(events, validCodes)

    val blocks   = CountryBlockSource.loadAllBlocks()
    val deltaMap = mutable.Map[(String, String), Int]()
    applyRelationEvents(events, deltaMap, blocks, validCodes)

    if (deltaMap.nonEmpty) {
      val safeDelta = deltaMap.filter { case ((c1, c2), _) => validCodes.contains(c1) && validCodes.contains(c2) }
      println(s"  patching ${safeDelta.size} pairs")
      CountrySource.patchCountryMutualRelationships(safeDelta)
    }
    println(s"[WorldStateGenerator] cycle $cycle DONE")
  }

  // ── Batch 1: block events ─────────────────────────────────────────────────────

  private def isBlockEvent(action: EventAction): Boolean = action match {
    case EventAction.CreateBlock      | EventAction.DissolveBlock |
         EventAction.JoinBlock        | EventAction.LeaveBlock    |
         EventAction.SetBlockValue    | EventAction.SetBlockEnforce |
         EventAction.SetBlockRelations => true
    case _ => false
  }

  private def applyBlockEvents(events: List[GeopoliticalEvent], validCodes: Set[String]): Unit =
    events.filter(e => isBlockEvent(e.action)).foreach(applyBlockEvent(_, validCodes))

  private def applyBlockEvent(event: GeopoliticalEvent, validCodes: Set[String]): Unit = {
    event.action match {

      case EventAction.CreateBlock =>
        event.value match {
          case None =>
            println(s"  WARN [CREATE_BLOCK]: missing value for '${event.origin}', skipping")
          case Some(rv) =>
            // target field: "US,CA,GB" or "US,CA,GB|enforce"
            val (membersPart, enforce) = event.target.split('|').toList match {
              case members :: "enforce" :: _ => (members, true)
              case members :: _              => (members, false)
              case Nil                       => ("", false)
            }
            val members = CountryBlock.parseCsv(membersPart).filter { c =>
              val ok = validCodes.contains(c)
              if (!ok) println(s"  WARN [CREATE_BLOCK]: unknown member '$c', skipping")
              ok
            }
            CountryBlockSource.saveBlock(CountryBlock(event.origin, clamp(rv), members, enforce))
            println(s"  EVENT [CREATE_BLOCK]: '${event.origin}' rv=$rv members=${members.size} enforce=$enforce")
        }

      case EventAction.DissolveBlock =>
        CountryBlockSource.loadBlockByName(event.origin) match {
          case None    => println(s"  WARN [DISSOLVE_BLOCK]: unknown block '${event.origin}', skipping")
          case Some(_) =>
            CountryBlockSource.deleteBlock(event.origin)   // CASCADE removes block relations too
            println(s"  EVENT [DISSOLVE_BLOCK]: '${event.origin}' dissolved")
        }

      case EventAction.JoinBlock =>
        if (!validCodes.contains(event.origin)) {
          println(s"  WARN [JOIN_BLOCK]: unknown country '${event.origin}', skipping")
        } else {
          CountryBlockSource.loadBlockByName(event.target) match {
            case None =>
              println(s"  WARN [JOIN_BLOCK]: unknown block '${event.target}', skipping")
            case Some(block) =>
              CountryBlockSource.addMember(event.target, event.origin)
              if (block.enforceRelations)
                accumulateEnforcementForMember(event.origin, event.target, validCodes)
              println(s"  EVENT [JOIN_BLOCK]: '${event.origin}' -> '${event.target}'${if (block.enforceRelations) " (enforcement queued)" else ""}")
          }
        }

      case EventAction.LeaveBlock =>
        if (!validCodes.contains(event.origin)) {
          println(s"  WARN [LEAVE_BLOCK]: unknown country '${event.origin}', skipping")
        } else {
          CountryBlockSource.loadBlockByName(event.target) match {
            case None =>
              println(s"  WARN [LEAVE_BLOCK]: unknown block '${event.target}', skipping")
            case Some(block) if !block.members.contains(event.origin) =>
              println(s"  WARN [LEAVE_BLOCK]: '${event.origin}' not in '${event.target}', skipping")
            case Some(_) =>
              CountryBlockSource.removeMember(event.target, event.origin)
              println(s"  EVENT [LEAVE_BLOCK]: '${event.origin}' left '${event.target}'")
          }
        }

      case EventAction.SetBlockValue =>
        // Sets intra-block relation_value to an absolute target
        event.value match {
          case None =>
            println(s"  WARN [SET_BLOCK_VALUE]: missing value for '${event.origin}', skipping")
          case Some(v) =>
            CountryBlockSource.loadBlockByName(event.origin) match {
              case None =>
                println(s"  WARN [SET_BLOCK_VALUE]: unknown block '${event.origin}', skipping")
              case Some(block) =>
                val newValue = clamp(v)
                CountryBlockSource.saveBlock(block.copy(relationValue = newValue))
                println(s"  EVENT [SET_BLOCK_VALUE]: '${event.origin}' relation_value -> $newValue")
            }
        }

      case EventAction.SetBlockEnforce =>
        event.value match {
          case None =>
            println(s"  WARN [SET_BLOCK_ENFORCE]: missing value for '${event.origin}', skipping")
          case Some(v) if v != 0 && v != 1 =>
            println(s"  WARN [SET_BLOCK_ENFORCE]: value must be 0 or 1 for '${event.origin}', got $v, skipping")
          case Some(v) =>
            CountryBlockSource.loadBlockByName(event.origin) match {
              case None =>
                println(s"  WARN [SET_BLOCK_ENFORCE]: unknown block '${event.origin}', skipping")
              case Some(block) =>
                val enforce = v == 1
                CountryBlockSource.saveBlock(block.copy(enforceRelations = enforce))
                println(s"  EVENT [SET_BLOCK_ENFORCE]: '${event.origin}' enforce_relations -> $enforce")
                // When enabling, propagate existing block relations to all current members
                if (enforce) {
                  block.members.foreach(m => accumulateEnforcementForMember(m, event.origin, validCodes))
                  println(s"    enforcement queued for ${block.members.size} members")
                }
            }
        }

      case EventAction.SetBlockRelations =>
        event.value match {
          case None =>
            println(s"  WARN [SET_BLOCK_RELATIONS]: missing value for '${event.origin}' -> '${event.target}', skipping")
          case Some(v) =>
            CountryBlockSource.loadBlockByName(event.origin) match {
              case None =>
                println(s"  WARN [SET_BLOCK_RELATIONS]: unknown block '${event.origin}', skipping")
              case Some(block) =>
                val clamped = clamp(v)
                CountryBlockSource.saveBlockRelation(CountryBlockRelation(event.origin, event.target, clamped))
                println(s"  EVENT [SET_BLOCK_RELATIONS]: '${event.origin}' -> '${event.target}' = $clamped")
                if (block.enforceRelations) {
                  block.members.foreach(m => accumulateEnforcementForMember(m, event.origin, validCodes))
                  println(s"    enforcement queued for ${block.members.size} members")
                }
            }
        }

      case _ =>
    }
  }

  // ── Enforcement accumulator ───────────────────────────────────────────────────

  /**
   * Reads a block's current country_block_relation entries and merges them into
   * enforcementAccumulator for the given member. Max absolute value wins on conflict.
   * Does not write to the relation table — flush happens in applyRelationEvents.
   */
  private def accumulateEnforcementForMember(
    memberCode: String,
    blockName : String,
    validCodes: Set[String]
  ): Unit = {
    val blockRelations = CountryBlockSource.loadBlockRelations(blockName)
    val allBlocks      = CountryBlockSource.loadAllBlocks()
    val blockMemberMap = allBlocks.map(b => b.blockName -> b.members.toSet).toMap

    blockRelations.foreach { rel =>
      val targetCodes = resolveEntities(rel.target, validCodes, blockMemberMap, "ENFORCEMENT")
      targetCodes.foreach { targetCode =>
        if (targetCode != memberCode) {
          val key     = orderedPair(memberCode, targetCode)
          val current = enforcementAccumulator.get(key)
          val winner  = current match {
            case None    => rel.relationValue
            case Some(c) => if (math.abs(rel.relationValue) >= math.abs(c)) rel.relationValue else c
          }
          enforcementAccumulator(key) = winner
        }
      }
    }
  }

  // ── Full relation map builder (init only) ────────────────────────────────────

  private def buildFullRelationMap(
    blocks    : List[CountryBlock],
    validCodes: Set[String]
  ): mutable.Map[(String, String), Int] = {
    val map = computeBlockBaseline(blocks, validCodes)
    validCodes.foreach(code => map((code, code)) = RELATION_MAX)
    map
  }

  // ── Batch 2: relation override events + enforcement flush ─────────────────────

  private def applyRelationEvents(
    events    : List[GeopoliticalEvent],
    map       : mutable.Map[(String, String), Int],
    blocks    : List[CountryBlock],
    validCodes: Set[String]
  ): Unit = {
    val blockMemberMap = blocks.map(b => b.blockName -> b.members.toSet).toMap

    // Flush enforcement accumulator — these came from batch 1 JOIN/SET_BLOCK_ENFORCE/SET_BLOCK_RELATIONS
    enforcementAccumulator.foreach { case ((a, b), v) =>
      map((a, b)) = v
      map((b, a)) = v
    }
    enforcementAccumulator.clear()

    // SET_RELATIONS — country origins only, block-floor-respecting
    events.filter(_.action == EventAction.SetRelations).foreach { event =>
      event.value match {
        case None =>
          println(s"  WARN [SET_RELATIONS]: event ${event.id} missing value, skipping")
        case Some(v) =>
          val tokens = event.origin.split(",").map(_.trim).filter(_.nonEmpty)
          val invalid = tokens.filterNot(validCodes.contains)
          if (invalid.nonEmpty) {
            println(s"  WARN [SET_RELATIONS]: event ${event.id} origin contains non-country tokens: ${invalid.mkString(",")}, skipping")
          } else {
            val originCodes = tokens.toSet
            val targetCodes = resolveEntities(event.target, validCodes, blockMemberMap, "SET_RELATIONS")
            val clamped     = clamp(v)
            originCodes.foreach { a =>
              targetCodes.foreach { b =>
                if (a != b) {
                  val floor = blockBaseline(a, b, blocks)
                  if (clamped > floor) { map((a, b)) = clamped; map((b, a)) = clamped }
                }
              }
            }
            if (originCodes.nonEmpty && targetCodes.nonEmpty)
              println(s"  EVENT [SET_RELATIONS]: ${event.origin} x ${event.target} -> $clamped")
          }
      }
    }

    // SET_RELATIONS_OVERRIDE — any origin, unconditional
    events.filter(_.action == EventAction.SetRelationsOverride).foreach { event =>
      event.value match {
        case None =>
          println(s"  WARN [SET_RELATIONS_OVERRIDE]: event ${event.id} missing value, skipping")
        case Some(v) =>
          val originCodes = resolveEntities(event.origin, validCodes, blockMemberMap, "SET_RELATIONS_OVERRIDE")
          val targetCodes = resolveEntities(event.target, validCodes, blockMemberMap, "SET_RELATIONS_OVERRIDE")
          val clamped     = clamp(v)
          originCodes.foreach { a =>
            targetCodes.foreach { b =>
              if (a != b) { map((a, b)) = clamped; map((b, a)) = clamped }
            }
          }
          if (originCodes.nonEmpty && targetCodes.nonEmpty)
            println(s"  EVENT [SET_RELATIONS_OVERRIDE]: ${event.origin} x ${event.target} -> $clamped (${originCodes.size * targetCodes.size} pairs)")
      }
    }
  }

  // ── Helpers ───────────────────────────────────────────────────────────────────

  private def computeBlockBaseline(
    blocks    : List[CountryBlock],
    validCodes: Set[String]
  ): mutable.Map[(String, String), Int] = {
    val map = mutable.Map[(String, String), Int]()
    blocks.foreach { block =>
      val members = block.members.filter(validCodes.contains)
      members.foreach { a =>
        members.foreach { b =>
          if (a != b) {
            val current = map.getOrElse((a, b), Int.MinValue)
            if (block.relationValue > current) map((a, b)) = block.relationValue
          }
        }
      }
    }
    map
  }

  private def blockBaseline(a: String, b: String, blocks: List[CountryBlock]): Int =
    blocks
      .filter(bl => bl.members.contains(a) && bl.members.contains(b))
      .map(_.relationValue)
      .foldLeft(0)(math.max)

  private def resolveEntities(
    ref           : String,
    validCodes    : Set[String],
    blockMemberMap: Map[String, Set[String]],
    label         : String
  ): Set[String] =
    ref.split(",").map(_.trim).filter(_.nonEmpty).flatMap { token =>
      blockMemberMap.get(token) match {
        case Some(members) => members
        case None =>
          if (validCodes.contains(token)) Set(token)
          else { println(s"  WARN [$label]: unresolvable entity '$token', skipping"); Set.empty[String] }
      }
    }.toSet

  private def orderedPair(a: String, b: String): (String, String) =
    if (a < b) (a, b) else (b, a)

  private def clamp(value: Int): Int = value.max(RELATION_MIN).min(RELATION_MAX)
}