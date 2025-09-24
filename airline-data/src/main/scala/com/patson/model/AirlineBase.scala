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

  val COST_EXPONENTIAL_BASE = 1.6  
  // This value governs base upkeep and build cost scaling from level. 
  // 1.7 means bases above lvl 10 are hardly sustainable w/o assets. Use lower values to make it more affordable. 
  
  lazy val getUpkeep : Long = {
    val adjustedScale = if (scale == 0) 1 else scale //for non-existing base, calculate as if the base is 1
    var baseUpkeep = (5000 + airport.rating.overallRating * 150).toLong

    (baseUpkeep * airportSizeRatio * Math.pow(COST_EXPONENTIAL_BASE, adjustedScale - 1)).toInt
  }

  // This Defines Discounts to upkeep & build costs for lower scale airpots!
  lazy val airportSizeRatio =
    //Upkeep Costs for Airport Scales below 6 are as follows:
    // Scale 6 and up: 100%
    // Scale 5:        80%
    // Scale 4:        70%
    // Scale 3:        60%
    // Scale 2:        50%
    // Scale 1:        40%
    if (airport.size > 6) {
      1.0
    } else {
      0.3 + airport.size * 0.1
    }

//  def getLinkLimit(titleOption : Option[Title.Value]) : Int = {
//    val base = 5
//    val titleBonus = titleOption match {
//      case Some(title) => CountryAirlineTitle.getLinkLimitBonus(title)
//      case None => 0
//    }
//
//    val scaleBonus =
//      if (headquarter) {
//        4 * scale
//      } else {
//        2 * scale
//      }
//
//    base + titleBonus + scaleBonus
//  }

  val getOfficeStaffCapacity = AirlineBase.getOfficeStaffCapacity(scale, headquarter)

//  val HQ_BASIC_DELEGATE = 7
//  val NON_HQ_BASIC_DELEGATE = 3
//  val delegateCapacity : Int =
//    (if (headquarter) HQ_BASIC_DELEGATE else NON_HQ_BASIC_DELEGATE) + scale / (if (headquarter) 1 else 2)

  val delegatesRequired : Int = {
    if (headquarter) {
      scale / 2
    } else {
      1 + scale / 2
    }
  }


  def getOvertimeCompensation(staffRequired : Int) = {
    if (getOfficeStaffCapacity >= staffRequired) {
      0
    } else {
      val delta = staffRequired - getOfficeStaffCapacity
      var compensation = 0
      val income = CountrySource.loadCountryByCode(countryCode).map(_.income).getOrElse(0)
      compensation += delta * (50000 + income) / 52 * 10 //weekly compensation, *10, as otherwise it's too low

      compensation
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
  def getOfficeStaffCapacity(scale : Int, isHeadquarters : Boolean) = {
    val base =
      if (isHeadquarters) {
        60
      } else {
        0
      }
    val scaleBonus =
      if (isHeadquarters) {
        80 * scale
      } else {
        60 * scale
      }

    base + scaleBonus
  }
}




