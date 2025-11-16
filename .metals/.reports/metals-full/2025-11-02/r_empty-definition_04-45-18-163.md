error id: file://<WORKSPACE>/airline-data/src/main/scala/com/patson/model/Lounge.scala:scala/Enumeration#Value#
file://<WORKSPACE>/airline-data/src/main/scala/com/patson/model/Lounge.scala
empty definition using pc, found symbol in pc: scala/Enumeration#Value#
empty definition using semanticdb
empty definition using fallback
non-local guesses:
	 -Value#
	 -scala/Predef.Value#
offset: 4305
uri: file://<WORKSPACE>/airline-data/src/main/scala/com/patson/model/Lounge.scala
text:
```scala
package com.patson.model

/* ===
  Future planned rework of lounges: 

case class Lounge(
  airline : Airline,
  allianceId : Option[Int],
  airport : Airport,
  name : String = "",
  level : Int,
  status : LoungeStatus.Value,
  foundedCycle : Int
  capacity : Int,                              // absolute pax limit per week
  condition: Double,                           // use Lounge.MAX_CONDITION when constructing new lounges
)
=== */
case class Lounge(
  airline : Airline, 
  allianceId : Option[Int], 
  airport : Airport, 
  name : String = "", 
  level : Int, 
  status : LoungeStatus.Value, 
  foundedCycle : Int) {
  
  
  def getValue : Long = {level * 50000000 }
  //def getValue : Long = { baseValue + features.map(_.cost).sum } -- Future: includes features
  
  val getUpkeep : Long = {
    if (status == LoungeStatus.ACTIVE) (10000 + airport.baseIncome) * 5 * level else 0 
    //use base income for calculation here
    //future: multiply upkeep by feature upkeepModifier?
  }
  
  //This is logic for percieved airplane ticket price reduction! 
  //To be further adjusted if needed with extra levels!
  val baseReduceRate = 0.005 + level * 0.01
  val getPriceReduceFactor : (Int => Double) = flightDistance => -1 * (baseReduceRate * Math.max(0.5, Math.min(1.0, flightDistance / 10000.0)))
  
}
/* ===
Future planned rework of lounge features:

case class LoungeFeature(
  id: Int,
  name: String,
  description: String,
  category: LoungeFeatureCategory.Value,
  cost: Long,                    // one-time cost
  upkeepModifier: Double = 1.0,  // affects lounge upkeep
  capacityBonus: Int = 0,        // increases lounge capacity
  satisfactionBonus: Double = 0, // future: adds to pax satisfaction
  drawback: Option[String] = None
)

object LoungeFeatureCategory extends Enumeration {
  type LoungeFeatureCategory = Value
  val Usual, CapacityUpgrade, Unique = Value
}

object LoungeFeatureLibrary {
  // Placeholder library of features — later move to DB or JSON
  val DefaultFeatures: List[LoungeFeature] = List(
    LoungeFeature(
      1,
      "Premium Drinks Bar",
      "Adds comfort and prestige.",
      LoungeFeatureCategory.Usual,
      cost = 1_000_000,
      upkeepModifier = 1.1,
      satisfactionBonus = 0.05
    ),
    LoungeFeature(
      2,
      "Dedicated Wi-Fi Zone",
      "Improves business traveler satisfaction.",
      LoungeFeatureCategory.Usual,
      cost = 500_000,
      upkeepModifier = 1.05
    ),
    LoungeFeature(
      3,
      "Capacity Expansion",
      "Increases maximum lounge capacity.",
      LoungeFeatureCategory.CapacityUpgrade,
      cost = 2_000_000,
      capacityBonus = 200
    ),
    LoungeFeature(
      4,
      "Signature Suite",
      "Unique luxury area for VIP pax.",
      LoungeFeatureCategory.Unique,
      cost = 5_000_000,
      upkeepModifier = 1.3,
      satisfactionBonus = 0.1
    )
  )
}
=== */

/* ===
  Future planned rework of lounges:

object Lounge {
  val PER_VISITOR_COST = 50 
  val PER_VISITOR_CHARGE = 100 //Maybe let it be user-set if we later let players share lounges w/o alliance?
  val MAX_LEVEL = 6

  val LOUNGE_PASSENGER_AIRPORT_SIZE_REQUIREMENT = 4 //This is for demand generator, not important!

  def getBaseScaleRequirement(loungeLevel : int) = {
    if (loungeLevel == 6) {
      15
    } else if (loungeLevel == 5) {
      12
    } else if (loungeLevel == 4) {
      9
    } else if (loungeLevel == 3) {
      7
    } else if (loungeLevel == 2) {
      5
    } else {3
    }
  }
}

=== */


object Lounge {
  val PER_VISITOR_COST = 50 //how much extra cost to serve 1 visitor
  val PER_VISITOR_CHARGE = 100 //how much to charge an airline (self and alliance member) per 1 visitor. This has to be higher to make popular lounge profitable
  val MAX_LEVEL = 3
  
  val LOUNGE_PASSENGER_AIRPORT_SIZE_REQUIREMENT = 4 //lounge passenger only spawn if from and to airport fulfills this
  
  def getBaseScaleRequirement(loungeLevel : Int) = {
    if (loungeLevel == 3) {
      11
    } else if (loungeLevel == 2) {
      9
    } else {
      7
    }
  }
}

/* === 
  Future Planned Rework of Lounges:

object LoungeStatus extends Enumeration {
  type LoungeStatus = Value
  val ACTIVE, INACTIVE, UNDER_COSTRUCTION = Value
}
=== */
object LoungeStatus extends Enumeration {
  type LoungeStatus = V@@alue
  val ACTIVE, INACTIVE = Value
}
```


#### Short summary: 

empty definition using pc, found symbol in pc: scala/Enumeration#Value#