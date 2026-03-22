package com.patson.util

import com.patson.data.CountrySource

object RelationshipCache {

  @volatile private var cache: Map[(String, String), Int] = Map.empty

  def reload(): Unit = {
    cache = CountrySource.getCountryMutualRelationships()
    println(s"[RelationshipCache] loaded ${cache.size} mutual relationship pairs")
  }

  /**
   * Returns the mutual relationship value between two countries.
   * Checks both (a,b) and (b,a) since the table may store only one direction.
   * Returns 0 (Neutral) if no entry exists.
   */
  def getRelationship(a: String, b: String): Int =
    cache.getOrElse((a, b), cache.getOrElse((b, a), 0))

  /**
   * True if the two country codes share a domestic market (relationship >= 5)
   * or are the same country.
   */
  def isSameMarket(a: String, b: String): Boolean =
    a == b || getRelationship(a, b) >= 5

}