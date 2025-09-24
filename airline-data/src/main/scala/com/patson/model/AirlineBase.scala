package com.patson.model

import com.patson.data.{AirlineSource, AirportSource, CountrySource}
import com.patson.model.AirlineBaseSpecialization.FlightTypeSpecialization
import com.patson.util.AirportCache


case class AirlineBase(airline : Airline, airport : Airport, countryCode : String, scale : Int, foundedCycle : Int, headquarter : Boolean = false) {
  lazy val getValue : Long = {
    if (scale == 0) {
      0
    } else if (headquarter && scale == 1) { //free to start HQ
      0
    } else {
      var baseCost = (1000000 + airport.rating.overallRating * 120000).toLong
      (baseCost * airportSizeRatio * Math.pow (COST_EXPONENTIAL_BASE, (scale - 1) )).toLong
    }
  }

  // This value governs base upkeep and build cost scaling from level. 
  // 1.7 means bases above lvl 10 are hardly sustainable w/o assets. Use lower values to make it more affordable. 
  val COST_EXPONENTIAL_BASE = 1.6  
  
  lazy val getUpkeep : Long = {
    val adjustedScale = if (scale == 0) 1 else scale //for non-existing base, calculate as if the base is 1
    var baseUpkeep = (5000 + airport.rating.overallRating * 150).toLong

    (baseUpkeep * airportSizeRatio * Math.pow(COST_EXPONENTIAL_BASE, adjustedScale - 1)).toInt
  }

  // This Defines Discounts to upkeep & build costs for lower scale airpots!
  // For each scale below scale 7 get 10% discount: 
  // Scale 6 = 10% / 90% of baseline cost, Scale 1 = 60% / 40% of baseline cost.
  lazy val airportSizeRatio =
    if (airport.size > 6) {
      1.0
    } else {
      0.3 + airport.size * 0.1
    }

  // Logic inside "AirlineBase" object on line 124
  val getOfficeStaffCapacity = AirlineBase.getOfficeStaffCapacity(scale, headquarter)

  // Delegate Cost of a Base / HQ (Now equalised!)
  val delegatesRequired : Int = scale / 2

  // Overtime Cost Logic
  val EXP_OVERTIME_FACTOR = 1.5  // Exponential growth factor for overtime tiers

  def getOvertimeCompensation(staffRequired : Int) = {
    if (getOfficeStaffCapacity >= staffRequired) {
      0
    } else {
      val delta = staffRequired - getOfficeStaffCapacity
      val income = CountrySource.loadCountryByCode(countryCode).map(_.income).getOrElse(0)
      val baseCostPerStaff = (50000 + income) / 52 * 10  // Weekly compensation per staff, *10 as in original
      val tierSize = AirlineBase.STAFF_PER_SCALE(headquarter) // "AirlineBase.STAFF_PER_SCALE" can be found on lines 125-127

      var totalCompensation = 0.0  // Use Double for precision with exponents
      var remainingDelta = delta
      var tier = 0  // Starting tier (0 for first tierSize staff)

      while (remainingDelta > 0) {
        val currentTierSize = Math.min(tierSize, remainingDelta)  // Handle partial tiers
        val tierMultiplier = Math.pow(EXP_OVERTIME_FACTOR, tier)  // Exponential growth: factor^tier
        totalCompensation += currentTierSize * baseCostPerStaff * tierMultiplier

        remainingDelta -= tierSize
        tier += 1
      }

      totalCompensation.toLong  // Convert back to Long for return type consistency
    }
  }

  /**
    * if not allowed, return LEFT[the title required]
    */
  lazy val allowAirline : Airline => Either[Title.Value, Title.Value]= (airline : Airline) => {

    val requiredTitle =
      if (airport.isGateway()) {
        Title.ESTABLISHED_AIRLINE
      } else {
        Title.PRIVILEGED_AIRLINE
      }
    val title = CountryAirlineTitle.getTitle(airport.countryCode, airline)
    if (title.title.id <= requiredTitle.id) { //lower id means higher title
      Right(requiredTitle)
    } else {
      Left(requiredTitle)
    }
  }

  lazy val getStaffModifier : (FlightCategory.Value => Double) = flightCategory => {
    val flightTypeSpecializations = specializations.filter(_.getType == BaseSpecializationType.FLIGHT_TYPE).map(_.asInstanceOf[FlightTypeSpecialization])
    if (flightTypeSpecializations.isEmpty) {
      1
    } else {
      flightTypeSpecializations.map(_.staffModifier(flightCategory)).sum - (flightTypeSpecializations.size - 1)
    }
  }

  lazy val specializations : List[AirlineBaseSpecialization.Value] = {
    (AirlineBaseSpecialization.values.filter(_.free).toList ++
    AirportSource.loadAirportBaseSpecializations(airport.id, airline.id)).filter(_.scaleRequirement <= scale)
  }

  def delete(): Unit = {
    AirlineSource.loadLoungeByAirlineAndAirport(airline.id, airport.id).foreach { lounge =>
      AirlineSource.deleteLounge(lounge)
    }

    //remove all base spec and bonus since it has no foreign key on base
    specializations.foreach { spec =>
      spec.unapply(airline, airport)
    }
    AirportSource.updateAirportBaseSpecializations(airport.id, airline.id, List.empty)
    //then delete the base itself
    AirlineSource.deleteAirlineBase(this)
  }
}

object AirlineBase {
  val STAFF_PER_SCALE: Map[Boolean, Int] = Map(
    true -> 80,   // Headquarters: 80 staff per scale or tier
    false -> 60   // Non-headquarters: 60 staff per scale or tier
  )

  def getOfficeStaffCapacity(scale : Int, isHeadquarters : Boolean) = {
    val base =
      if (isHeadquarters) {
        60
      } else {
        0
      }
    val scaleBonus = STAFF_PER_SCALE(isHeadquarters) * scale

    base + scaleBonus
  }
}




