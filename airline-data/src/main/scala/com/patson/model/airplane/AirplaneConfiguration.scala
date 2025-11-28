package com.patson.model.airplane
import com.patson.model.{AbstractLinkClassValues, Airline, BUSINESS, FIRST, LinkClassValues, ECONOMY}
import com.patson.model.airplane.Model.Type

object AirplaneConfiguration {
  val empty = AirplaneConfiguration(0, 0, 0, Airline.fromId(0), Model.fromId(0), true)

  val default: ((Airline, Model) => AirplaneConfiguration) = (airline, model) => {
    val baseConfig = if (model.airplaneType == Type.SUPERSONIC) {
      val galleySpace = getGalleySpace(Type.SUPERSONIC)
      val effectiveCapacity = model.capacity - galleySpace
      val defaultBusiness = (effectiveCapacity / BUSINESS.spaceMultiplier).toInt
      AirplaneConfiguration(economyVal = 0, businessVal = defaultBusiness, firstVal = 0, airline, model, true)
    } else {
      AirplaneConfiguration(economyVal = model.capacity, businessVal = 0, firstVal = 0, airline, model, true)
    }
    if (!baseConfig.isValid) {
      throw new IllegalArgumentException(s"Invalid default configuration for model ${model.name} of type ${model.airplaneType}")
    }
    baseConfig
  }

  val MAX_CONFIGURATION_TEMPLATE_COUNT = 5 // per model and airline

  def validated(economyVal: Int, businessVal: Int, firstVal: Int, airline: Airline, model: Model, isDefault: Boolean, id: Int = 0): AirplaneConfiguration = {
    val config = AirplaneConfiguration(economyVal, businessVal, firstVal, airline, model, isDefault, id)
    if (!config.isValid) {
      throw new IllegalArgumentException(s"Invalid configuration for ${model.airplaneType}: Check allowed classes, minimum seats, or capacity (including galleys if premium classes are present).")
    }
    config
  }

  // Helper methods for restrictions
  def isBusinessAllowed(airplaneType: Type.Value): Boolean = {
    airplaneType match {
      case Type.LIGHT | Type.SMALL => false
      case _ => true
    }
  }

  def isFirstAllowed(airplaneType: Type.Value): Boolean = {
    airplaneType match {
      case Type.LIGHT | Type.SMALL | Type.REGIONAL | Type.SUPERSONIC => false
      case _ => true
    }
  }

  val minBusinessMap: Map[Type.Value, Int] = Map(
    Type.REGIONAL -> 3,
    Type.MEDIUM -> 4,
    Type.LARGE -> 6,
    Type.X_LARGE -> 8,
    Type.JUMBO -> 8,
    Type.SUPERSONIC -> 6 // Adjustable based on typical supersonic capacities
  )

  def getMinBusiness(airplaneType: Type.Value): Int = minBusinessMap.getOrElse(airplaneType, 0)

  val minFirstMap: Map[Type.Value, Int] = Map(
    Type.MEDIUM -> 2,
    Type.LARGE -> 4,
    Type.X_LARGE -> 4,
    Type.JUMBO -> 4,
    Type.SUPERSONIC -> 0 // Not allowed, so 0
  )

  def getMinFirst(airplaneType: Type.Value): Int = minFirstMap.getOrElse(airplaneType, 0)

  val galleySpaceMap: Map[Type.Value, Int] = Map(
    Type.REGIONAL -> 10, // 5x2
    Type.MEDIUM -> 12,   // 6x2
    Type.LARGE -> 16,    // 8x2
    Type.X_LARGE -> 20,  // 10x2
    Type.JUMBO -> 20,    // 10x2
    Type.SUPERSONIC -> 16 // Adjustable, assuming similar to LARGE
  )

  def getGalleySpace(airplaneType: Type.Value): Int = galleySpaceMap.getOrElse(airplaneType, 0)
}

case class AirplaneConfiguration(economyVal: Int, businessVal: Int, firstVal: Int, airline: Airline, model: Model, isDefault: Boolean, var id: Int = 0) extends AbstractLinkClassValues(economyVal, businessVal, firstVal) {
  import AirplaneConfiguration._ // Import companion object members for access

  lazy val minimized: AirplaneConfiguration = {
    val hasPremium = businessVal > 0 || firstVal > 0
    val galleySpace = if (hasPremium) getGalleySpace(model.airplaneType) else 0
    val effectiveCapacity = model.capacity - galleySpace

    val minimizedFirst = if (isFirstAllowed(model.airplaneType)) Math.max(getMinFirst(model.airplaneType), (effectiveCapacity / FIRST.spaceMultiplier).toInt) else 0
    val remainingAfterFirst = effectiveCapacity - (minimizedFirst * FIRST.spaceMultiplier)
    val minimizedBusiness = if (isBusinessAllowed(model.airplaneType)) Math.max(getMinBusiness(model.airplaneType), (remainingAfterFirst / BUSINESS.spaceMultiplier).toInt) else 0

    AirplaneConfiguration(0, minimizedBusiness, minimizedFirst, airline, model, isDefault)
  }

  def isValid: Boolean = {
    // Check allowed classes
    if (!isBusinessAllowed(model.airplaneType) && businessVal > 0) return false
    if (!isFirstAllowed(model.airplaneType) && firstVal > 0) return false

    // Additional restriction for SUPERSONIC: only Business allowed, no Economy or First
    if (model.airplaneType == Type.SUPERSONIC && (economyVal > 0 || firstVal > 0)) return false

    // Check minimum seats (only if class is present)
    if (businessVal > 0 && businessVal < getMinBusiness(model.airplaneType)) return false
    if (firstVal > 0 && firstVal < getMinFirst(model.airplaneType)) return false

    // Check capacity with dynamic galley adjustment
    val hasPremium = businessVal > 0 || firstVal > 0
    val galleySpace = if (hasPremium) getGalleySpace(model.airplaneType) else 0
    val seatSpace = (economyVal * ECONOMY.spaceMultiplier) + (businessVal * BUSINESS.spaceMultiplier) + (firstVal * FIRST.spaceMultiplier)
    if (seatSpace + galleySpace > model.capacity) return false

    true
  }
}

/* FULL REFACTORED VERSION FOR INCLUSION OF PREMIUM ECONOMY!

package com.patson.model.airplane
import com.patson.model.{AbstractLinkClassValues, Airline, BUSINESS, FIRST, PREMIUM_ECONOMY, LinkClassValues}

case class AirplaneConfiguration(economyVal: Int, premiumEconomyVal: Int, businessVal: Int, firstVal: Int, airline: Airline, model: Model, isDefault: Boolean, var id: Int = 0)
  extends AbstractLinkClassValues(economyVal, premiumEconomyVal, businessVal, firstVal) {
  
  lazy val minimized: AirplaneConfiguration = { // config that has least capacity
    val minimizedFirst = (model.capacity / FIRST.spaceMultiplier).toInt
    val minimizedBusiness = ((model.capacity - minimizedFirst * FIRST.spaceMultiplier) / BUSINESS.spaceMultiplier).toInt
    val minimizedPremiumEconomy = ((model.capacity - minimizedFirst * FIRST.spaceMultiplier - minimizedBusiness * BUSINESS.spaceMultiplier) / PREMIUM_ECONOMY.spaceMultiplier).toInt
    // no eco as user can lock econ to zero
    AirplaneConfiguration(0, minimizedPremiumEconomy, minimizedBusiness, minimizedFirst, airline, model, isDefault)
  }
}

object AirplaneConfiguration {
  val empty = AirplaneConfiguration(0, 0, 0, 0, Airline.fromId(0), Model.fromId(0), true)
  val default: ((Airline, Model) => AirplaneConfiguration) = (airline, model) => AirplaneConfiguration(economyVal = model.capacity, 0, 0, 0, airline, model, true)
  val MAX_CONFIGURATION_TEMPLATE_COUNT = 5 // per model and airline
}

 */