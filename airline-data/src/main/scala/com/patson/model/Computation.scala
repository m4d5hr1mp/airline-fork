package com.patson.model
import com.patson.PassengerSimulation.LINK_COST_TOLERANCE_FACTOR
import com.patson.model.airplane._
import com.patson.model.airplane.Model.Type._
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
      result(i) = Computation.internalComputeStandardFlightDuration(i)
    }
    result
  }
  def getMaxValues(): (Long, Long, Long) = {
    val allAirports = AirportSource.loadAllAirports()
    //take note that below should NOT use boosted values, should use base, otherwise it will incorrectly load some lazy vals of the Airport that is MAX
    (allAirports.maxBy(_.basePower).basePower, allAirports.maxBy(_.basePopulation).basePopulation, allAirports.maxBy(_.baseIncome).baseIncome)
  }

  // distance vs max speed (restored from older version for simple duration calculations)
  val speedLimits = List((300, 350), (400, 500), (400, 700))

  // Simple duration calculation (restored from older version for standard/reference use)
  def calculateSimpleDuration(airplaneSpeed : Int, distance : Int) : Int = {
    var remainDistance = distance
    var duration = 0
    for ((distanceBucket, maxSpeed) <- speedLimits if (remainDistance > 0)) {
      val speed = Math.min(maxSpeed, airplaneSpeed)
      if (distanceBucket >= remainDistance) {
        duration += remainDistance * 60 / speed
      } else {
        duration += distanceBucket * 60 / speed
      }
      remainDistance -= distanceBucket
    }
    if (remainDistance > 0) {
      duration += remainDistance * 60 / airplaneSpeed
    }
    duration
  }

  case class FlightPhases(climbTimeMin: Double, cruiseTimeMin: Double, descentTimeMin: Double, groundOpsMin: Double)

  /** Type-specific climb + fuel parameters */
  private case class ClimbParams(
    baseRateMMin: Double,        // m/min at sea level
    maxCruiseAltM: Double,       // target cruise altitude
    decayScaleM: Double          // how fast climb rate falls off
  )

  private val climbParamsByType: Map[Model.Type.Type, ClimbParams] = Map(
    SHORT_RANGE_PROP -> ClimbParams(720,  7600,  6200),  // early props
    LONG_RANGE_PROP  -> ClimbParams(740,  8200,  6500),  // DC-6/7, Constellation
    SMALL_PROP       -> ClimbParams(680,  7600,  6000),
    REGIONAL_PROP    -> ClimbParams(750,  8500,  6800),

    LIGHT            -> ClimbParams(450, 13000, 9500),   // light jets
    SMALL            -> ClimbParams(620, 11500, 9200),   // CRJ/E-Jet small
    REGIONAL         -> ClimbParams(680, 11800, 9500),   // E170–E195, CRJ700+
    MEDIUM           -> ClimbParams(850, 12500, 10800),  // A320/B737 family

    EARLY_JET        -> ClimbParams(650, 11000, 8500),   // Comet, 707, DC-8, Caravelle

    LARGE            -> ClimbParams(920, 13500,  7800),  // B767, A300/310
    X_LARGE          -> ClimbParams(950, 14000,  7200),  // A330, B777-200, A350-900
    JUMBO            -> ClimbParams(960, 14000,  6800),  // 747, A380, B777-300ER/9

    SUPERSONIC       -> ClimbParams(1250,18000,  9500)
  )

  def calculateFlightPhases(airplaneModel: Model, distanceKm: Int): FlightPhases = {
    val speedKph = airplaneModel.speed.toDouble
    val params = climbParamsByType.getOrElse(
      airplaneModel.airplaneType, 
      ClimbParams(700, 11000, 9000)   // fallback default
    )

    // Short-haul altitude cap for all jets (< 500 km)
    val effectiveCruiseAltM = if (distanceKm < 500 && !airplaneModel.airplaneType.toString.contains("PROP")) {
      math.min(params.maxCruiseAltM, 7600.0)
    } else {
      params.maxCruiseAltM
    }

    val baseClimbRate = params.baseRateMMin
    val decayScale = params.decayScaleM

    // Analytical climb time with exponential decay: rate(alt) = base * exp(-alt / scale)
    val climbTimeMin = (decayScale / baseClimbRate) * (math.exp(effectiveCruiseAltM / decayScale) - 1)
    val descentTimeMin = climbTimeMin * 0.85   // descent is slightly faster

    // Ground distance covered during climb + descent (average 60% of cruise speed)
    val avgClimbDescentSpeedKph = speedKph * 0.60
    val climbDescentDistanceKm = avgClimbDescentSpeedKph * (climbTimeMin + descentTimeMin) / 60.0

    // Cruise phase
    val cruiseDistanceKm = math.max(0.0, distanceKm - climbDescentDistanceKm)
    val cruiseTimeMin = if (cruiseDistanceKm > 0) {
      cruiseDistanceKm / (speedKph / 60.0)
    } else {
      0.0
    }

    // Special case: very short route with no meaningful cruise phase
    val finalClimbTimeMin = if (cruiseTimeMin < 1.0) {
      // Treat entire flight as 50% climb + 50% descent at average rate
      val totalTimeMin = distanceKm / (speedKph * 0.75 / 60.0)   // average 75% of cruise speed
      totalTimeMin * 0.5
    } else {
      climbTimeMin
    }

    val finalDescentTimeMin = if (cruiseTimeMin < 1.0) finalClimbTimeMin else descentTimeMin

    // Ground operations (taxi + holding buffer)
    val groundOpsMin = 30.0

    FlightPhases(finalClimbTimeMin, cruiseTimeMin, finalDescentTimeMin, groundOpsMin)
  }

  def calculateDuration(airplaneModel: Model, distance: Int): Int = {
    val phases = calculateFlightPhases(airplaneModel, distance)
    (phases.climbTimeMin + phases.cruiseTimeMin + phases.descentTimeMin + phases.groundOpsMin).toInt
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
    // val distance = distanceOption.getOrElse(Util.calculateDistance(fromAirport.latitude, fromAirport.longitude, toAirport.latitude, toAirport.longitude).toInt)
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
      var compensation = (affectedSeatsPerFlight * link.cancellationCount * 0.5 * link.price).total //50% of ticket price, as there's some penalty for that already
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
    Computation.calculateSimpleDuration(standardSpeed, distance)
  }
  def getDomesticAirportWithinRange(principalAirport : Airport, range : Int) = { //range in km
    val affectedAirports = ListBuffer[Airport]()
    AirportSource.loadAirportsByCountry(principalAirport.countryCode).foreach { airport =>
      if (Computation.calculateDistance(principalAirport, airport) <= range) {
        affectedAirports.append(airport)
      }
    }
    affectedAirports.toList
  }
}