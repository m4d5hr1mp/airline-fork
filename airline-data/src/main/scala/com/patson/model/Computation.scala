package com.patson.model
import com.patson.PassengerSimulation.LINK_COST_TOLERANCE_FACTOR
import com.patson.model.airplane._
import com.patson.model.airplane.Model.Type._
import com.patson.data.{AirlineSource, AirplaneSource, AirportAssetSource, AirportSource, AllianceSource, BankSource, CountrySource, CycleSource, OilSource}
import com.patson.Util
import com.patson.util.{AirlineCache, AllianceRankingUtil, RelationshipCache}
import scala.collection.mutable.ListBuffer

object Computation {
  val MODEL_COUNTRY_CODE = "US"
  lazy val MODEL_COUNTRY_POWER: Double = CountrySource.loadCountryByCode(MODEL_COUNTRY_CODE) match {
    case Some(country) => country.airportPopulation.toDouble * country.income
    case None =>
      println(s"Cannot find $MODEL_COUNTRY_CODE to compute model power")
      1
  }
  lazy val MAX_VALUES: (Long, Long, Long) = getMaxValues()
  lazy val MODEL_AIRPORT_POWER: Long = MAX_VALUES._1
  lazy val MAX_POPULATION: Long      = MAX_VALUES._2
  lazy val MAX_INCOME: Long          = MAX_VALUES._3
  val MAX_COMPUTED_DISTANCE = 20000

  lazy val standardFlightDurationCache: Array[Int] = {
    val result = new Array[Int](MAX_COMPUTED_DISTANCE + 1)
    for (i <- 0 to MAX_COMPUTED_DISTANCE) {
      result(i) = internalComputeStandardFlightDuration(i)
    }
    result
  }

  def getMaxValues(): (Long, Long, Long) = {
    val allAirports = AirportSource.loadAllAirports()
    (allAirports.maxBy(_.basePower).basePower,
     allAirports.maxBy(_.basePopulation).basePopulation,
     allAirports.maxBy(_.baseIncome).baseIncome)
  }

  // ── Simple duration (unchanged, used for standard/reference pricing) ──────
  val speedLimits = List((300, 350), (400, 500), (400, 700))

  def calculateSimpleDuration(airplaneSpeed: Int, distance: Int): Int = {
    var remainDistance = distance
    var duration = 0
    for ((distanceBucket, maxSpeed) <- speedLimits if remainDistance > 0) {
      val speed = math.min(maxSpeed, airplaneSpeed)
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

  // ── Flight phase calculation ───────────────────────────────────────────────
  case class FlightPhases(
    climbTimeMin: Double,
    cruiseTimeMin: Double,
    descentTimeMin: Double,
    groundOpsMin: Double
  )

  /**
   * Exponential climb-rate decay constant (metres).
   * Represents how quickly the climb rate falls off with altitude.
   * A single conservative middle-of-range constant avoids needing a
   * per-model column for a second-order effect.
   */
  private val CLIMB_DECAY_SCALE_M = 8500.0

  /**
   * Compute climb / cruise / descent phases from per-model stored parameters.
   *
   * @param airplaneModel  Model with climbRateMMin and cruiseAltitudeM populated from DB.
   * @param distanceKm     Great-circle distance of the route in km.
   */
  def calculateFlightPhases(airplaneModel: Model, distanceKm: Int): FlightPhases = {
    val speedKph        = airplaneModel.speed.toDouble
    val baseClimbRate   = airplaneModel.climbRateMMin.toDouble   // m/min at sea level
    val fullCruiseAltM  = airplaneModel.cruiseAltitudeM.toDouble // stored target cruise altitude

    // Short-haul altitude cap for jets on routes < 500 km — they never reach full cruise FL.
    val isProp = airplaneModel.airplaneType match {
      case SHORT_RANGE_PROP | LONG_RANGE_PROP | SMALL_PROP | REGIONAL_PROP => true
      case _ => false
    }
    val effectiveCruiseAltM =
      if (distanceKm < 500 && !isProp) math.min(fullCruiseAltM, 7600.0)
      else fullCruiseAltM

    // Analytical climb time with exponential decay of climb rate:
    //   rate(alt) = baseRate * exp(-alt / decayScale)
    //   climbTime  = integral from 0 to targetAlt of 1/rate(alt) dalt
    //              = (decayScale / baseRate) * (exp(targetAlt / decayScale) - 1)
    val climbTimeMin   = (CLIMB_DECAY_SCALE_M / baseClimbRate) *
                         (math.exp(effectiveCruiseAltM / CLIMB_DECAY_SCALE_M) - 1)
    val descentTimeMin = climbTimeMin * 0.85  // descent slightly faster

    // Ground distance covered during climb + descent (average 60% of cruise speed).
    val avgClimbDescentSpeedKph  = speedKph * 0.60
    val climbDescentDistanceKm   = avgClimbDescentSpeedKph * (climbTimeMin + descentTimeMin) / 60.0

    // Cruise phase.
    val cruiseDistanceKm = math.max(0.0, distanceKm - climbDescentDistanceKm)
    val cruiseTimeMin    = if (cruiseDistanceKm > 0) cruiseDistanceKm / (speedKph / 60.0) else 0.0

    // Very short routes with no meaningful cruise: treat as 50/50 climb/descent
    // at 75% of cruise speed throughout.
    val (finalClimbTimeMin, finalDescentTimeMin) =
      if (cruiseTimeMin < 1.0) {
        val totalTimeMin = distanceKm / (speedKph * 0.75 / 60.0)
        val half = totalTimeMin * 0.5
        (half, half)
      } else {
        (climbTimeMin, descentTimeMin)
      }

    FlightPhases(
      climbTimeMin   = finalClimbTimeMin,
      cruiseTimeMin  = cruiseTimeMin,
      descentTimeMin = finalDescentTimeMin,
      groundOpsMin   = 30.0  // taxi + holding buffer
    )
  }

  def calculateDuration(airplaneModel: Model, distance: Int): Int = {
    val phases = calculateFlightPhases(airplaneModel, distance)
    (phases.climbTimeMin + phases.cruiseTimeMin + phases.descentTimeMin + phases.groundOpsMin).toInt
  }

  def calculateFlightMinutesRequired(airplaneModel: Model, distance: Int): Int = {
    val duration = calculateDuration(airplaneModel, distance)
    (duration + airplaneModel.turnaroundTime) * 2
  }

  def calculateMaxFrequency(airplaneModel: Model, distance: Int): Int = {
    if (airplaneModel.range < distance) 0
    else (Airplane.MAX_FLIGHT_MINUTES / calculateFlightMinutesRequired(airplaneModel, distance)).toInt
  }

  val SELL_RATE = 0.8
  def calculateAirplaneSellValue(airplane: Airplane): Int = {
    val currentNewMarketPrice = airplane.model.applyDiscount(
      ModelDiscount.getBlanketModelDiscounts(airplane.model.id)).price
    val value = airplane.value * airplane.purchaseRate * SELL_RATE
    if (value < 0) 0 else value.toInt
  }

  def calculateDistance(fromAirport: Airport, toAirport: Airport): Int =
    Util.calculateDistance(fromAirport.latitude, fromAirport.longitude,
                           toAirport.latitude,   toAirport.longitude).toInt

  def getFlightType(fromAirport: Airport, toAirport: Airport): FlightType.Value =
    getFlightType(fromAirport, toAirport, calculateDistance(fromAirport, toAirport))

  def getFlightType(fromAirport: Airport, toAirport: Airport, distance: Int): FlightType.Value = {
    import FlightType._
    if (RelationshipCache.isSameMarket(fromAirport.countryCode, toAirport.countryCode)) {
      if (distance <= 1000)       SHORT_HAUL_DOMESTIC
      else if (distance <= 3000)  MEDIUM_HAUL_DOMESTIC
      else                        LONG_HAUL_DOMESTIC
    } else if (fromAirport.zone == toAirport.zone) {
      if (distance <= 2000)       SHORT_HAUL_INTERNATIONAL
      else if (distance <= 4000)  MEDIUM_HAUL_INTERNATIONAL
      else                        LONG_HAUL_INTERNATIONAL
    } else {
      if (distance <= 2000)        SHORT_HAUL_INTERCONTINENTAL
      else if (distance <= 5000)   MEDIUM_HAUL_INTERCONTINENTAL
      else if (distance <= 12000)  LONG_HAUL_INTERCONTINENTAL
      else                         ULTRA_LONG_HAUL_INTERCONTINENTAL
    }
  }

  def getIncomeLevel(income: Int): Double = {
    val level = math.log(income.toDouble / 500) / math.log(1.1)
    if (level < 1) 1 else level
  }

  def fromIncomeLevel(incomeLevel: Double): Int =
    (math.pow(math.E, incomeLevel * math.log(1.1)) * 500).toInt

  def computeIncomeBoostFromLevel(baseIncome: Int, boostLevel: Double): Int = {
    val newIncomeLevel    = getIncomeLevel(baseIncome) + boostLevel
    val incomeIncrement   = fromIncomeLevel(newIncomeLevel) - baseIncome
    val maxIncomeBoost    = (boostLevel * 10_000).toInt
    val minIncomeBoost    = (boostLevel * 2_500).toInt
    if (incomeIncrement < minIncomeBoost)     minIncomeBoost
    else if (incomeIncrement <= maxIncomeBoost) incomeIncrement
    else maxIncomeBoost
  }

  def getLinkCreationCost(from: Airport, to: Airport): Int = {
    val baseCost              = 100000 + (from.income + to.income)
    val minAirportSize        = math.min(from.size, to.size)
    val airportSizeMultiplier = math.pow(1.5, minAirportSize)
    val distance              = calculateDistance(from, to)
    val distanceMultiplier    = distance.toDouble / 5000
    val internationalMultiplier = if (from.countryCode == to.countryCode) 1 else 3
    (baseCost * airportSizeMultiplier * distanceMultiplier * internationalMultiplier).toInt
  }

  val REDUCED_COMPENSATION_SERVICE_LEVEL_THRESHOLD = 40

  def computeCompensation(link: Link): Int = {
    if (link.majorDelayCount > 0 || link.minorDelayCount > 0 || link.cancellationCount > 0) {
      val soldSeatsPerFlight    = link.soldSeats / link.frequency
      val halfCapacityPerFlight = link.capacity / link.frequency * 0.5
      val affectedSeatsPerFlight =
        if (soldSeatsPerFlight.total > halfCapacityPerFlight.total) soldSeatsPerFlight
        else halfCapacityPerFlight
      var compensation =
        (affectedSeatsPerFlight * link.cancellationCount * 0.5 * link.price).total +
        (affectedSeatsPerFlight * link.majorDelayCount   * 0.3 * link.price).total +
        (affectedSeatsPerFlight * link.minorDelayCount   * 0.05 * link.price).total
      if (link.airline.getCurrentServiceQuality() < REDUCED_COMPENSATION_SERVICE_LEVEL_THRESHOLD) {
        val ratio = 0.2 + 0.8 * link.airline.getCurrentServiceQuality() /
                    REDUCED_COMPENSATION_SERVICE_LEVEL_THRESHOLD
        (compensation * ratio).toInt
      } else {
        compensation.toInt
      }
    } else 0
  }

  def getResetAmount(airlineId: Int): ResetAmountInfo = {
    val currentCycle         = CycleSource.loadCycle()
    val amountFromAirplanes  = AirplaneSource.loadAirplanesByOwner(airlineId, false)
                                 .map(calculateAirplaneSellValue(_).toLong).sum
    val amountFromBases      = AirlineSource.loadAirlineBasesByAirline(airlineId)
                                 .map(_.getValue * 0.2).sum.toLong
    val amountFromAssets     = AirportAssetSource.loadAirportAssetsByAirline(airlineId)
                                 .map(_.sellValue).sum
    val amountFromLoans      = BankSource.loadLoansByAirline(airlineId)
                                 .map(_.earlyRepayment(currentCycle) * -1).sum
    val amountFromOilContracts = OilSource.loadOilContractsByAirline(airlineId)
                                 .map(_.contractTerminationPenalty(currentCycle) * -1).sum
    val existingBalance      = AirlineCache.getAirline(airlineId).get.airlineInfo.balance
    ResetAmountInfo(amountFromAirplanes, amountFromBases, amountFromAssets,
                    amountFromLoans, amountFromOilContracts, existingBalance)
  }

  case class ResetAmountInfo(
    airplanes: Long, bases: Long, assets: Long,
    loans: Long, oilContracts: Long, existingBalance: Long
  ) {
    val overall: Long = airplanes + bases + assets + loans + oilContracts + existingBalance
  }

  val MAX_SATISFACTION_PRICE_RATIO_THRESHOLD = 0.7
  val MIN_SATISFACTION_PRICE_RATIO_THRESHOLD: Double = LINK_COST_TOLERANCE_FACTOR + 0.05

  val computePassengerSatisfaction: (Double, Int) => Double = (cost, standardPrice) => {
    val ratio = cost / standardPrice
    val s = (MIN_SATISFACTION_PRICE_RATIO_THRESHOLD - ratio) /
            (MIN_SATISFACTION_PRICE_RATIO_THRESHOLD - MAX_SATISFACTION_PRICE_RATIO_THRESHOLD)
    math.min(1, math.max(0, s))
  }

  val computeStandardFlightDuration: Int => Int = distance =>
    if (distance <= MAX_COMPUTED_DISTANCE) standardFlightDurationCache(distance)
    else {
      println(s"Unexpected distance $distance")
      internalComputeStandardFlightDuration(distance)
    }

  private def internalComputeStandardFlightDuration(distance: Int): Int = {
    val standardSpeed =
      if (distance <= 1000)      400
      else if (distance <= 2000) 600
      else                       800
    calculateSimpleDuration(standardSpeed, distance)
  }

  def getDomesticAirportWithinRange(principalAirport: Airport, range: Int): List[Airport] = {
    val affected = ListBuffer[Airport]()
    AirportSource.loadAirportsByCountry(principalAirport.countryCode).foreach { airport =>
      if (calculateDistance(principalAirport, airport) <= range) affected.append(airport)
    }
    affected.toList
  }
}