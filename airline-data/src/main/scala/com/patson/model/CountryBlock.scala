package com.patson.model

/**
 * Materialized current state of a geopolitical/economic bloc.
 *
 * @param blockName        Unique bloc identifier
 * @param relationValue    Baseline affinity between all member pairs (-5..5)
 * @param members          Current member list, stored as CSV in DB
 * @param enforceRelations If true, the bloc's external relations (from country_block_relation)
 *                         are automatically propagated to all member countries on JOIN_BLOCK
 *                         and on any SET_BLOCK_RELATIONS update. Use for tightly coordinated
 *                         blocs (NATO, CSTO). Leave false for economic/trade blocs (ASEAN, CPTPP).
 */
case class CountryBlock(
  blockName       : String,
  relationValue   : Int,
  members         : List[String],   // ISO-3166-1 alpha-2
  enforceRelations: Boolean = false
) {
  def membersAsCsv: String = members.mkString(",")
}

object CountryBlock {
  def parseCsv(csv: String): List[String] =
    csv.split(",").map(_.trim).filter(_.nonEmpty).toList
}

/**
 * A single external-relation entry for a bloc — what the bloc collectively
 * thinks of a target entity.
 *
 * @param blockName     The bloc this stance belongs to
 * @param target        Entity reference: ISO2 code or block name (resolved at runtime)
 * @param relationValue The stance value (-5..5)
 */
case class CountryBlockRelation(
  blockName    : String,
  target       : String,
  relationValue: Int
)