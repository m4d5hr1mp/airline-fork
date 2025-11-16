error id: file://<WORKSPACE>/airline-data/src/main/scala/com/patson/model/Computation.scala:`<none>`.
file://<WORKSPACE>/airline-data/src/main/scala/com/patson/model/Computation.scala
empty definition using pc, found symbol in pc: `<none>`.
empty definition using semanticdb
empty definition using fallback
non-local guesses:
	 -com/patson/model/airplane/principalAirport.
	 -com/patson/model/airplane/principalAirport#
	 -com/patson/model/airplane/principalAirport().
	 -principalAirport.
	 -principalAirport#
	 -principalAirport().
	 -scala/Predef.principalAirport.
	 -scala/Predef.principalAirport#
	 -scala/Predef.principalAirport().
offset: 13840
uri: file://<WORKSPACE>/airline-data/src/main/scala/com/patson/model/Computation.scala
text:
```scala
package com.patson.model

import com.patson.PassengerSimulation.LINK_COST_TOLERANCE_FACTOR
import com.patson.model.airplane._
import com.patson.data.{AirlineSource, AirplaneSource, AirportAssetSource, AirportSource, AllianceSource, BankSource, CountrySource, CycleSource, OilSource}
import com.patson.Util
import com.patson.util.{AirlineCache, AllianceRankingUtil}

import scala.collection.mutable.ListBuffer

object Computation {
  val MODEL_COUNTRY_CODE = "US"
  lazy val MODEL_COUNTRY_POWER : Double = CountrySource.loadCountryByCode(MODEL_COUNTRY_CODE) match {
    case Some(country) =>
      country.airportPopulation.toDouble * country.income
    case None =>
      println(s"Cannot find $MODEL_COUNTRY_CODE to compute model power")
      1
  }

  lazy val MAX_VALUES = getMaxValues()
  lazy val MODEL_AIRPORT_POWER = MAX_VALUES._1
  lazy val MAX_POPULATION = MAX_VALUES._2
  lazy val MAX_INCOME = MAX_VALUES._3

  val MAX_COMPUTED_DISTANCE = 20000
  lazy val standardFlightDurationCache : Array[Int] = {
    val result = new Array[Int](MAX_COMPUTED_DISTANCE + 1)
    for (i <- 0 to MAX_COMPUTED_DISTANCE) { //should cover everything...
      result(i) =  Computation.internalComputeStandardFlightDuration(i)
    }
    result
  }

  def getMaxValues(): (Long, Long, Long) = {
    val allAirports = AirportSource.loadAllAirports()
    //take note that below should NOT use boosted values, should use base, otherwise it will incorrectly load some lazy vals of the Airport that is MAX
    (allAirports.maxBy(_.basePower).basePower, allAirports.maxBy(_.basePopulation).basePopulation, allAirports.maxBy(_.baseIncome).baseIncome)
  }



  def calculateDuration(airplaneModel: Model, distance: Int): Int = {
  /**
  * Calculates total flight duration (in minutes) based on aircraft model and route distance.
  * 
  * The calculation includes:
  *  - Category-specific climb rates and cruise altitudes
  *  - Forced reduced cruise altitude for short routes (< 500 km)
  *  - Symmetrical descent time (same rate as climb)
  *  - 15 minutes added at both origin and destination for taxi/holding
  *
  * Notes:
  *  - Climb and descent are treated as linear phases (no acceleration modeled)
  *  - Jets are distinguished from props by their cruise speed (> 670 km/h)
  *  - Future improvement hooks for climb-rate decay are included as comments
  */
    import com.patson.model.airplane.Model.Category._

    val speedKph = airplaneModel.speed

    // Determine if aircraft is a prop or jet
    val isProp = speedKph <= 670

    // --- Assign cruise altitude by category ---
    val cruiseAltMeters = if (isProp) {
      6500.0
    } else airplaneModel.category match {
      case LIGHT        => 13000.0   // Business jets
      case SMALL        => 9750.0    // Small regional jets
      case REGIONAL     => 10000.0   // Larger regionals (e.g. E190)
      case MEDIUM       => 10500.0   // 737/A320 family
      case LARGE        => 11000.0   // A300/A330/767 class
      case X_LARGE      => 12000.0   // 777-200, A350-900
      case JUMBO        => 12000.0   // 747, 777-300, A380
      case SUPERSONIC   => 18000.0   // Concorde-class
      case _            => 10500.0
    }

    // --- Short-haul rule ---
    // Routes shorter than 500 km are capped at 7600 m max cruise altitude!
    val effectiveCruiseAlt = if (distance < 500) {
      math.min(cruiseAltMeters, 7600.0)
    } else {
      cruiseAltMeters
    }

    // --- Base climb/descent rate by category (m/min) ---
    val baseClimbRate = airplaneModel.category match {
      case LIGHT | SMALL        => 400.0
      case REGIONAL             => 600.0
      case MEDIUM               => 800.0
      case LARGE | X_LARGE      => 900.0
      case JUMBO                => 900.0
      case SUPERSONIC           => 1200.0
      case _                    => 700.0
    }

    // --- Optional: altitude-dependent climb rate decay (disabled for now) ---
    /*
    // Option A: exponential decay (smooth)
    val decayFactor = Math.exp(-effectiveCruiseAlt / 15000.0)
    val effectiveClimbRate = baseClimbRate * (0.5 + 0.5 * decayFactor)
    
    // Option B: simple piecewise decay (clear and adjustable)
    val effectiveClimbRate = if (effectiveCruiseAlt < 7600) {
      baseClimbRate
    } else if (effectiveCruiseAlt < 10000) {
      baseClimbRate * 0.8
    } else if (effectiveCruiseAlt < 12000) {
      baseClimbRate * 0.65
    } else {
      baseClimbRate * 0.55
    }
    */

    // For now, climb/descent rate remains constant
    val effectiveClimbRate = baseClimbRate
    val effectiveDescentRate = baseClimbRate // symmetrical descent

    // --- Calculate phase times ---
    val climbTimeMin   = effectiveCruiseAlt / effectiveClimbRate
    val descentTimeMin = effectiveCruiseAlt / effectiveDescentRate

    // Average climb/descent ground speed = 60% of cruise speed
    val climbDescentDistance = (speedKph * 0.6 / 60) * (climbTimeMin + descentTimeMin)
    val cruiseDistance = math.max(0, distance - climbDescentDistance)

    val cruiseTimeMin = if (cruiseDistance > 0) {
      cruiseDistance / (speedKph / 60.0)
    } else {
      0.0
    }

    // --- Add taxi/holding buffer (15 min each end) ---
    val groundOpsBufferMin = 30.0

    val totalTimeMin = climbTimeMin + cruiseTimeMin + descentTimeMin + groundOpsBufferMin

    totalTimeMin.toInt
  }

  def calculateFlightMinutesRequired(airplaneModel : Model, distance : Int) : Int = {
    val duration = calculateDuration(airplaneModel, distance)
    val roundTripTime = (duration + airplaneModel.turnaroundTime) * 2
    roundTripTime
  }

  def calculateMaxFrequency(airplaneModel : Model, distance : Int) : Int = {
    if (airplaneModel.range < distance) {
      0
    } else {
      val roundTripTime = calculateFlightMinutesRequired(airplaneModel, distance)
      (Airplane.MAX_FLIGHT_MINUTES / roundTripTime).toInt
    }
  }
  

  val SELL_RATE = 0.8
  
  def calculateAirplaneSellValue(airplane : Airplane) : Int = {
    val currentNewMarketPrice = airplane.model.applyDiscount(ModelDiscount.getBlanketModelDiscounts(airplane.model.id)).price
    val value = airplane.value * airplane.purchaseRate * SELL_RATE //airplane.purchase < 1 means it was bought with a discount, selling should be lower price
    if (value < 0) 0 else value.toInt
  }
  
  def calculateDistance(fromAirport : Airport, toAirport : Airport) : Int = {
    Util.calculateDistance(fromAirport.latitude, fromAirport.longitude, toAirport.latitude, toAirport.longitude).toInt
  }

  def getFlightType(fromAirport : Airport, toAirport : Airport) : FlightType.Value = {
    getFlightType(fromAirport, toAirport, calculateDistance(fromAirport, toAirport))
  }
  
  def getFlightType(fromAirport : Airport, toAirport : Airport, distance : Int) = { 
//    val distance = distanceOption.getOrElse(Util.calculateDistance(fromAirport.latitude, fromAirport.longitude, toAirport.latitude, toAirport.longitude).toInt)
    
    import FlightType._
    if (fromAirport.countryCode == toAirport.countryCode) { //domestic
      if (distance <= 1000) {
        SHORT_HAUL_DOMESTIC
      } else if (distance <= 3000) {
        MEDIUM_HAUL_DOMESTIC
      } else {
        LONG_HAUL_DOMESTIC
      }
    } else if (fromAirport.zone == toAirport.zone) { //international but same continent
      if (distance <= 2000) {
        SHORT_HAUL_INTERNATIONAL
      } else if (distance <= 4000) {
        MEDIUM_HAUL_INTERNATIONAL
      } else {
        LONG_HAUL_INTERNATIONAL
      }
    } else {
      if (distance <= 2000) {
        SHORT_HAUL_INTERCONTINENTAL
      } else if (distance <= 5000) {
        MEDIUM_HAUL_INTERCONTINENTAL
      } else if (distance <= 12000) {
        LONG_HAUL_INTERCONTINENTAL
      } else {
        ULTRA_LONG_HAUL_INTERCONTINENTAL
      }
    }
  }
  

  /**
   * Returns a normalized income level, should be greater than 0
   */
  def getIncomeLevel(income : Int) : Double = {
    val incomeLevel = (Math.log(income.toDouble / 500) / Math.log(1.1))
    if (incomeLevel < 1) {
      1
    } else {
      incomeLevel
    }
  }
  def fromIncomeLevel(incomeLevel : Double) : Int = {
    (Math.pow(Math.E, incomeLevel * Math.log(1.1)) * 500).toInt
  }


  /**
    * For low income base, use the boost level (which is MAX boost). For higher income base, down adjust it to certain
    * percentage
    * @param baseIncome
    * @param boostLevel
    * @return
    */
  def computeIncomeBoostFromLevel(baseIncome : Int, boostLevel : Double) = {
    val newIncomeLevel = getIncomeLevel(baseIncome) + boostLevel
    val incomeIncrement = fromIncomeLevel(newIncomeLevel) - baseIncome
    val maxIncomeBoost = (boostLevel * 10_000).toInt //a bit arbitrary
    val minIncomeBoost = (boostLevel * 2_500).toInt
    val finalBoost =
      if (incomeIncrement < minIncomeBoost) {
        minIncomeBoost
      } else if (incomeIncrement <= maxIncomeBoost) {
        incomeIncrement
      } else {
        maxIncomeBoost
      }

    finalBoost
  }

  
  def getLinkCreationCost(from : Airport, to : Airport) : Int = {
    
    val baseCost = 100000 + (from.income + to.income)
      
    val minAirportSize = Math.min(from.size, to.size) //encourage links for smaller airport
    
    val airportSizeMultiplier = Math.pow(1.5, minAirportSize) 
    val distance = calculateDistance(from, to)
    val distanceMultiplier = distance.toDouble / 5000
    val internationalMultiplier = if (from.countryCode == to.countryCode) 1 else 3
    
    (baseCost * airportSizeMultiplier * distanceMultiplier * internationalMultiplier).toInt 
  }
  
  val REDUCED_COMPENSATION_SERVICE_LEVEL_THRESHOLD = 40 //airline with service level below this will pay less compensation
  
  def computeCompensation(link : Link) : Int = {
    if (link.majorDelayCount > 0 || link.minorDelayCount > 0 || link.cancellationCount > 0 ) {
      val soldSeatsPerFlight = link.soldSeats / link.frequency
      val halfCapacityPerFlight = link.capacity / link.frequency * 0.5
      
      val affectedSeatsPerFlight = if (soldSeatsPerFlight.total > halfCapacityPerFlight.total) soldSeatsPerFlight else halfCapacityPerFlight //if less than 50% LF, considered that as 50% LF
      var compensation = (affectedSeatsPerFlight * link.cancellationCount * 0.5 * link.price).total  //50% of ticket price, as there's some penalty for that already
      compensation = compensation + (affectedSeatsPerFlight * link.majorDelayCount * 0.3 * link.price).total //30% of ticket price
      compensation = compensation + (affectedSeatsPerFlight * link.minorDelayCount * 0.05 * link.price).total //5% of ticket price

      if (link.airline.getCurrentServiceQuality() < REDUCED_COMPENSATION_SERVICE_LEVEL_THRESHOLD) { //down to only 20%
        val ratio = 0.2 + 0.8 * link.airline.getCurrentServiceQuality() / REDUCED_COMPENSATION_SERVICE_LEVEL_THRESHOLD
        (compensation * ratio).toInt
      } else {
        compensation.toInt
      }
    } else {
      0
    }
  }

  def getResetAmount(airlineId : Int) : ResetAmountInfo = {
    val currentCycle = CycleSource.loadCycle()
    val amountFromAirplanes = AirplaneSource.loadAirplanesByOwner(airlineId, false).map(Computation.calculateAirplaneSellValue(_).toLong).sum
    val amountFromBases = AirlineSource.loadAirlineBasesByAirline(airlineId).map(_.getValue * 0.2).sum.toLong //only get 20% back
    val amountFromAssets = AirportAssetSource.loadAirportAssetsByAirline(airlineId).map(_.sellValue).sum
    val amountFromLoans = BankSource.loadLoansByAirline(airlineId).map(_.earlyRepayment(currentCycle) * -1).sum //repay all loans now
    val amountFromOilContracts = OilSource.loadOilContractsByAirline(airlineId).map(_.contractTerminationPenalty(currentCycle) * -1).sum //termination penalty
    val existingBalance = AirlineCache.getAirline(airlineId).get.airlineInfo.balance
    
    ResetAmountInfo(amountFromAirplanes, amountFromBases, amountFromAssets, amountFromLoans, amountFromOilContracts, existingBalance)
  }
  
  case class ResetAmountInfo(airplanes : Long, bases : Long, assets : Long, loans : Long, oilContracts : Long, existingBalance : Long) {
    val overall = airplanes + bases + assets + loans + oilContracts + existingBalance
  }

  val MAX_SATISFACTION_PRICE_RATIO_THRESHOLD = 0.7 //at 100% satisfaction is <= this threshold
  val MIN_SATISFACTION_PRICE_RATIO_THRESHOLD = LINK_COST_TOLERANCE_FACTOR + 0.05 //0% satisfaction >= this threshold ... +0.05 so, there will be at least some satisfaction even at the LINK_COST_TOLERANCE_FACTOR
  /**
    * From 0 (not satisfied at all) to 1 (fully satisfied)
    *
    *
    */
  val computePassengerSatisfaction = (cost: Double, standardPrice : Int) => {
    val ratio = cost / standardPrice
    var satisfaction = (MIN_SATISFACTION_PRICE_RATIO_THRESHOLD - ratio) / (MIN_SATISFACTION_PRICE_RATIO_THRESHOLD - MAX_SATISFACTION_PRICE_RATIO_THRESHOLD)
    satisfaction = Math.min(1, Math.max(0, satisfaction))
    //println(s"${cost} vs standard price $standardPrice. satisfaction : ${satisfaction}")
    satisfaction
  }

  val computeStandardFlightDuration = (distance: Int) => {
    if (distance <= MAX_COMPUTED_DISTANCE) {
      standardFlightDurationCache(distance)
    } else {
      println(s"Unexpected distance $distance")
      internalComputeStandardFlightDuration(distance) //just in case
    }
  }
  private def internalComputeStandardFlightDuration(distance : Int) = {
    val standardSpeed =
      if (distance <= 1000) {
        400
      } else if (distance <= 2000) {
        600
      } else {
        800
      }
    Computation.calculateDuration(standardSpeed, distance)
  }

  def getDomesticAirportWithinRange(principalAirport : Airport, range : Int) = { //range in km
    val affectedAirports = ListBuffer[Airport]()
    AirportSource.loadAirportsByCountry(principalAirport.countryCode).foreach { airport =>
      if (Computation.calculateDistance(principalAi@@rport, airport) <= range) {
        affectedAirports.append(airport)
      }
    }
    affectedAirports.toList
  }
}
```


#### Short summary: 

empty definition using pc, found symbol in pc: `<none>`.