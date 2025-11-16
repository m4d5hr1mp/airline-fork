package com.patson.model

/* ===
//Future planned rework of lounges: 

case class Lounge(
  airline : Airline,
  allianceId : Option[Int],
  airport : Airport,
  name : String = "",
  level : Int,
  status : LoungeStatus.Value,
  capacity : Int,                              // absolute pax limit per week
  condition: Double,                           // use Lounge.MAX_CONDITION when constructing new lounges
  foundedCycle : Int
  var features: List[LoungeFeature] = List.empty
  ) {


  // Total lounge value, including features:
  def getValue : Long = {
    val baseValue = Lounge.getBaseValueByLevel(level) //Allows for non-linear, level-specific values! Check object definition!
    val featureValue = features.map(_.featureType.baseCost).sum
    ((baseValue + featureValue) * (condition / 100)).toLong //Adjust by condition for Renovations Cost Logic! 
  }

  // Regular upkeep scaled by lounge level, plus maintenance from active features.
  def getUpkeep : Long = {
    if (status == LoungeStatus.ACTIVE) {
      val baseUpkeep = (10000 + airport.baseIncome) * 5 * level
      val featureUpkeep = features.filter(_.isActive).map(_.featureType.maintenanceCost).sum
      (baseUpkeep + featureUpkeep).toLong
    } else 0
  }

  // Total Lounge Capacity Formula:
  def totalCapacity : Int = {
    val base = Lounge.LOUNGE_BASE_CAPACITY
    val levelBonus = (level - 1) * Lounge.LOUNGE_CAPACITY_INCREMENT
    val featureBonus = features.filter(_.isActive).map(_.featureType.capacityBonus).sum
    (base + levelBonus + featureBonus).toInt
  }

  // Lounge Condition Decay Logic! 100% -> 0% over 20*52 weeks!
  def loungeDecayCondition() : Lounge = {
    val decayPerWeek = 100.0 / (20 * 52).toDouble
    val newCondition = Math.max(0, condition - decayPerWeek)
    this.copy(condition = newCondition)
  }

  // Lounge renovations logic! Resets condition from current to MAX_Condition!
  def renovateLounge() : Lounge = {
    this.copy(condition = Lounge.MAX_CONDITION)
  }

  // Renovation Cost Formula:
  def getRenovationCost : Long = {
    val valueNow = getValue
    val valueFull = Lounge.getBaseValueByLevel(level) + features.map(_.featureType.baseCost).sum
    val depreciation = (valueFull - valueNow).max(0)
    (depreciation * 0.25).toLong // 25% of lost value as renovation cost
  }

}


//Future planned rework: Lounge Features (patterned after AirportAssetType)
object LoungeFeatureType extends Enumeration {
  abstract class LoungeFeatureType() extends super.Val {
    val label : String
    val descriptions : List[String]
    val constructionDuration : Int          // in weeks
    val baseCost : Long                     // cost to build or install
    val maintenanceCost : Long              // additional upkeep per week
    val capacityBonus : Int                 // adds to lounge capacity
    val unique : Boolean = false            // whether only one per lounge
    val featureLevelRequirement : Int       // minimum lounge level required
  }

  // Example features (flavor placeholders)
  case object PremiumBar extends LoungeFeatureType {
    val label = "Premium Bar"
    val descriptions = List("Adds luxury appeal and boosts satisfaction.")
    val constructionDuration = 26
    val baseCost = 5000000
    val maintenanceCost = 20000
    val capacityBonus = 20
    val featureLevelRequirement = 2
  }

  case object SpaFacility extends LoungeFeatureType {
    val label = "Spa Facility"
    val descriptions = List("Exclusive relaxation area for business and first-class travelers.")
    val constructionDuration = 52
    val baseCost = 10000000
    val maintenanceCost = 50000
    val capacityBonus = 40
    val featureLevelRequirement = 4
  }

  case object KidsZone extends LoungeFeatureType {
    val label = "Kids Zone"
    val descriptions = List("Family-friendly area that boosts satisfaction for leisure passengers.")
    val constructionDuration = 26
    val baseCost = 3000000
    val maintenanceCost = 10000
    val capacityBonus = 10
    val featureLevelRequirement = 3
  }

  case object CapacityExpansion extends LoungeFeatureType {
    val label = "Capacity Expansion"
    val descriptions = List("Dedicated space reconfiguration that increases lounge capacity.")
    val constructionDuration = 52
    val baseCost = 8000000
    val maintenanceCost = 15000
    val capacityBonus = 100
    val featureLevelRequirement = 2
  }

  val values = List(PremiumBar, SpaFacility, KidsZone, CapacityExpansion)
}

case class LoungeFeature(
  featureType : LoungeFeatureType.LoungeFeatureType,
  installedCycle : Int,
  var isActive : Boolean = true
)


//Future planned rework of lounges:
object Lounge {
  val PER_VISITOR_COST = 50 
  val PER_VISITOR_CHARGE = 100 //Maybe let it be user-set if we later let players share lounges w/o alliance?

  val MAX_LEVEL = 6
  val MAX_CONDITION = 100.0

  val LOUNGE_BASE_CAPACITY = 2500
  val LOUNGE_CAPACITY_INCREMENT = 5000 // Per level!

  // Base value formula by level (placeholder scaling curve)
  def getBaseValueByLevel(level : Int) : Long = level match {
    case 1 => 50_000_000
    case 2 => 100_000_000
    case 3 => 175_000_000
    case 4 => 275_000_000
    case 5 => 400_000_000
    case 6 => 600_000_000
    case _ => 0
  }

  val LOUNGE_PASSENGER_AIRPORT_SIZE_REQUIREMENT = 4 //This is for demand generator, not important!

  //Declare Base Levels for Lounge Upgrades!
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

//Base Capacity Formula:
}

//Future Planned Rework of Lounges:
object LoungeStatus extends Enumeration {
  type LoungeStatus = Value
  val ACTIVE, INACTIVE, UNDER_COSTRUCTION = Value
}
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

object LoungeStatus extends Enumeration {
  type LoungeStatus = Value
  val ACTIVE, INACTIVE = Value
}