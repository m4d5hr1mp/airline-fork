package com.patson.model

/**
 * A single entry in the geopolitical event log.
 *
 * ALL numeric values in events are ABSOLUTE TARGET values, not deltas.
 * "US-CN value=4" means set the relation TO 4, not add 4.
 *
 * @param id             DB primary key (0 for unsaved)
 * @param year           Game year — stored for human readability; not used for scheduling
 * @param week           Game week 1-52; -1 for init events
 * @param executionCycle Computed via ChronologicalConverter.toCycle(year, week); -1 for init
 * @param origin         Entity reference — constraints vary by action (see EventAction)
 * @param action         What happens
 * @param target         Entity reference — any type for all actions unless noted
 * @param value          Absolute target value. Required for CREATE_BLOCK, SET_BLOCK_VALUE,
 *                       SET_BLOCK_ENFORCE, SET_BLOCK_RELATIONS, SET_RELATIONS,
 *                       SET_RELATIONS_OVERRIDE. None for JOIN/LEAVE/DISSOLVE.
 */
case class GeopoliticalEvent(
  id            : Int,
  year          : Int,
  week          : Int,
  executionCycle: Int,
  origin        : String,
  action        : EventAction,
  target        : String,
  value         : Option[Int] = None  // absolute target value, never a delta
)

// ── Event Action ADT ──────────────────────────────────────────────────────────

sealed abstract class EventAction(val entryName: String)

object EventAction {

  // ── Block lifecycle ─────────────────────────────────────────────────────────

  /**
   * origin  = block name
   * target  = CSV of initial member ISO2 codes, optionally suffixed with |enforce
   *           e.g. "US,CA,GB" or "US,CA,GB|enforce"
   * value   = intra-block relation_value (absolute)
   */
  case object CreateBlock extends EventAction("CREATE_BLOCK")

  /**
   * origin  = block name
   * target  = ignored
   * value   = ignored
   * Cascade deletes country_block_relation rows for this block.
   */
  case object DissolveBlock extends EventAction("DISSOLVE_BLOCK")

  /**
   * origin  = ISO2 country code
   * target  = block name
   * value   = ignored
   * If block has enforce_relations=true, inherits block's external relations immediately.
   */
  case object JoinBlock extends EventAction("JOIN_BLOCK")

  /**
   * origin  = ISO2 country code
   * target  = block name
   * value   = ignored
   * Any previously enforced values from this block are superseded by remaining block memberships
   * on next full recompute. Use SET_RELATIONS events in the same cycle for explicit corrections.
   */
  case object LeaveBlock extends EventAction("LEAVE_BLOCK")

  // ── Block property setters ───────────────────────────────────────────────────

  /**
   * Sets the intra-block relation_value to an absolute target.
   * origin  = block name
   * target  = ignored
   * value   = new absolute intra-block relation_value (-5..5)
   */
  case object SetBlockValue extends EventAction("SET_BLOCK_VALUE")

  /**
   * Enables or disables foreign policy enforcement for a block.
   * origin  = block name
   * target  = ignored
   * value   = 1 (enable) or 0 (disable)
   * When enabling, immediately propagates all existing country_block_relation entries
   * for this block to all current members.
   */
  case object SetBlockEnforce extends EventAction("SET_BLOCK_ENFORCE")

  // ── Block-level external relations ──────────────────────────────────────────

  /**
   * Sets a bloc's collective stance toward a target entity (absolute value).
   * origin  = block name only
   * target  = any entity reference (ISO2, block name, or CSV mix)
   * value   = absolute relation value this bloc holds toward target (-5..5)
   * If block has enforce_relations=true, propagates to all current members.
   * Conflict resolution when multiple enforcing blocks affect same pair: max absolute value wins.
   */
  case object SetBlockRelations extends EventAction("SET_BLOCK_RELATIONS")

  // ── Direct relation overrides ────────────────────────────────────────────────

  /**
   * Sets bilateral relations to an absolute target value — block-floor-respecting.
   * origin  = ISO2 code(s) only — comma-separated, NO block names allowed
   * target  = any entity reference
   * value   = absolute target relation value; only applied if value > block baseline for that pair
   */
  case object SetRelations extends EventAction("SET_RELATIONS")

  /**
   * Sets bilateral relations to an absolute target value — unconditional.
   * origin  = any entity reference (ISO2, block name, or mix)
   * target  = any entity reference
   * value   = absolute target relation value; ignores block primacy
   * Same power as block enforcement — last applied in event log order wins.
   */
  case object SetRelationsOverride extends EventAction("SET_RELATIONS_OVERRIDE")

  val all: List[EventAction] = List(
    CreateBlock, DissolveBlock, JoinBlock, LeaveBlock,
    SetBlockValue, SetBlockEnforce, SetBlockRelations,
    SetRelations, SetRelationsOverride
  )

  def fromString(s: String): Option[EventAction] = all.find(_.entryName == s)
}