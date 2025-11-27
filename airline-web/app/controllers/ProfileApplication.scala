package controllers

import java.util.Random
import com.patson.data.{AirlineSource, AirplaneSource, AirportSource, BankSource, CycleSource, TutorialSource}
import com.patson.model._
import com.patson.model.airplane._
import com.patson.util.AirportCache
import controllers.AuthenticationObject.AuthenticatedAirline
import javax.inject.Inject
import models.Profile
import play.api.libs.json.{JsValue, Json, _}
import play.api.mvc._
import scala.collection.mutable.ListBuffer

object ProfileConfigs {
  case class PlaneConfig(
    budget: Long,
    condition: Double,
    allowedFamilies: List[String] = List.empty,
    maxLifespan: Option[Int] = None,
    maxSpeed: Option[Int] = None
  )

  case class LoanConfig(
    rate: Double,
    term: Int,
    isBasedOnPlaneCost: Boolean = false,
    principalFixed: Long = 0L,
    principalFraction: Double = 1.0
  )

  case class Config(
    id: Int,
    name: String,
    description: String,
    cashBase: Long,
    planeConfigs: List[PlaneConfig] = List.empty,
    reputation: Int,
    loanConfigs: List[LoanConfig] = List.empty,
    unspentToCashFraction: Double = 0.0,
    difficultyToCashFraction: Double = 0.0,
    difficultyToPlaneFractions: List[Double] = List.empty,
    currentServiceQuality: Double = 0.0,
    isAvailable: Airport => Boolean = _ => true
  )

  val specificIatas = List("DXB", "AUH", "DOH", "BAH", "KWI", "JED", "SIN", "KUL", "BKK", "HKG", "TPE", "TLV", "IST")
  val bushExtendedCountries = List("AU", "CA", "NZ")

  val all: List[Config] = List(
    // Profile 0: Cash Start
    Config(
      id = 0,
      name = "Cash Start",
      description = "Start with only cash in the bank, no aircraft or debt. Build your airline from scratch. Recomended for new players.",
      cashBase = 500000000L,
      planeConfigs = List.empty,
      reputation = 0,
      loanConfigs = List.empty,
      unspentToCashFraction = 0.0,
      difficultyToCashFraction = 1.0,
      difficultyToPlaneFractions = List.empty,
      currentServiceQuality = 0.0,
      isAvailable = _ => true
    ),


    // Profile 1: Flag Carrier
    Config(
      id = 1,
      name = "Flag Carrier",
      description = "Establish a prestigious national airline with a modern fleet supported by government subsidies. Moderate difficulty.",
      cashBase = 500000000L,
      planeConfigs = List(
        PlaneConfig(2000000000L, 100.0, List("Boeing 787","Boeing 777","Airbus A350","Airbus A330","Airbus A380","Boeing 747")),
        PlaneConfig(1000000000L, 100.0, List("Airbus A320","Boeing 737"))
      ),
      reputation = 50,
      loanConfigs = List(
        LoanConfig(rate = 0.0, term = 1024, isBasedOnPlaneCost = true, principalFraction = 1.0)
      ),
      unspentToCashFraction = 0.0,
      difficultyToCashFraction = 0.0,
      difficultyToPlaneFractions = List(1.0, 0.0),
      currentServiceQuality = 60.0,
      isAvailable = airport => specificIatas.contains(airport.iata)
    ),


    // Profile 2: Bargain Deal
    Config(
      id = 2,
      name = "Bargain Deal",
      description = "Acquire a fleet of used aircraft at a discount. Easy Difficulty",
      cashBase = 100000000L,
      planeConfigs = List(
        PlaneConfig(400000000L, 50.0, List("McDonnell Douglas MD-90","Fokker","Boeing 737 Classic","Avro RJ"))
      ),
      reputation = 30,
      loanConfigs = List(
        LoanConfig(rate = 0.1, term = 512, isBasedOnPlaneCost = true, principalFraction = 1.0)
      ),
      unspentToCashFraction = 0.5,
      difficultyToCashFraction = 0.0,
      difficultyToPlaneFractions = List(0.5),
      currentServiceQuality = 30.0,
      isAvailable = airport => airport.size >= 4 && airport.size <= 7
    ),


    // Profile 3: Revival of Past Glory
    Config(
      id = 3,
      name = "Revival of Past Glory",
      description = "Revive a once-great airline with a large fleet of widebody and narrowbody aircraft, but burdened with significant debt. Hard Difficulty",
      cashBase = 550000000L,
      planeConfigs = List(
        PlaneConfig(1100000000L, 60.0, List("Boeing 777","Boeing 767","Boeing 757","Boeing 747","Airbus A340","Airbus A330","Airbus A300/A310"), maxLifespan = Some(1560)),
        PlaneConfig(1200000000L, 60.0, List("Airbus A320","Boeing 737","Comac C919","Tupolev Tu-204","McDonnell Douglas MD-90"), maxLifespan = Some(1560))
      ),
      reputation = 70,
      loanConfigs = List(
        LoanConfig(rate = 0.10, term = 104, isBasedOnPlaneCost = false, principalFixed = 80000000L),
        LoanConfig(rate = 0.08, term = 208, isBasedOnPlaneCost = false, principalFixed = 170000000L),
        LoanConfig(rate = 0.05, term = 1024, isBasedOnPlaneCost = false, principalFixed = 590000000L),
        LoanConfig(rate = 0.11, term = 1024, isBasedOnPlaneCost = false, principalFixed = 390000000L),
        LoanConfig(rate = 0.07, term = 520, isBasedOnPlaneCost = false, principalFixed = 340000000L)
      ),
      unspentToCashFraction = 0.0,
      difficultyToCashFraction = 0.0,
      difficultyToPlaneFractions = List(0.75, 0.75),
      currentServiceQuality = 50.0,
      isAvailable = airport => airport.size >= 6
    ),


    // Profile 4: Regional Airline
    Config(
      id = 4,
      name = "Regional Airline",
      description = "Focus on short haul routes with a small of fleet regional aircraft. Easy difficulty",
      cashBase = 50000000L,
      planeConfigs = List(
        PlaneConfig(400000000L, 70.0, List("Sukhoi Superjet 100", "Fokker","Embraer ERJ", "Comac ARJ", "Bombardier DHC-8","Bombardier CRJ","Avro RJ","ATR-Regional"))
      ),
      reputation = 30,
      loanConfigs = List(
        LoanConfig(rate = 0.10, term = 260, isBasedOnPlaneCost = true, principalFraction = 0.4)
      ),
      unspentToCashFraction = 1.0,
      difficultyToCashFraction = 0.25,
      difficultyToPlaneFractions = List(0.25),
      currentServiceQuality = 40.0,
      isAvailable = airport => airport.size >= 4 && airport.size <= 5
    ),


    // Profile 5: Bush Airline
    Config(
      id = 5,
      name = "Bush Airline",
      description = "Operate in remote areas with a few small aircraft, ideal for the low-demand nature of bush airlines. Difficulty varries",
      cashBase = 50000000L,
      planeConfigs = List(
        PlaneConfig(175000000L, 70.0, List("ATR-Regional","Avro RJ","Bombardier DHC-8","Fokker","Embraer ERJ"), maxSpeed = Some(700))
      ),
      reputation = 20,
      loanConfigs = List(
        LoanConfig(rate = 0.08, term = 260, isBasedOnPlaneCost = true, principalFraction = 0.2)
      ),
      unspentToCashFraction = 1.0,
      difficultyToCashFraction = 0.20,
      difficultyToPlaneFractions = List(0.10),
      currentServiceQuality = 30.0,
      isAvailable = airport => {
        val maxScale = if (bushExtendedCountries.contains(airport.countryCode)) 4 else 3
        airport.size >= 1 && airport.size <= maxScale
      }
    )
  )
}

class ProfileApplication @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  implicit object ProfileWrites extends Writes[Profile] {
    def writes(profile: Profile): JsValue = {
      var result = Json.obj(
        "name" -> profile.name,
        "description" -> profile.description,
        "cash" -> profile.cash,
        "airplanes" -> profile.airplanes,
        "reputation" -> profile.reputation,
        "airportText" -> profile.airport.displayText
      )
      if (profile.loans.nonEmpty) {
        result = result + ("loans" -> JsArray(profile.loans.map(Json.toJson(_)(new LoanWrites(CycleSource.loadCycle())))))
      }
      result
    }
  }

  val BONUS_PER_DIFFICULTY_POINT = 5000000L

  def generateAirplanes(budget: Long, homeAirport: Airport, condition: Double, airline: Airline, random: Random, allowedFamilies: List[String] = List.empty, maxLifespan: Option[Int] = None, maxSpeed: Option[Int] = None): List[Airplane] = {
    val eligibleModels = allAirplaneModels
      .filter(model => model.purchasableWithRelationship(allCountryRelationships.getOrElse((homeAirport.countryCode, model.countryCode), 0)))
      .filter(model => model.price * condition / Airplane.MAX_CONDITION <= budget / 3)
      .filter(model => model.runwayRequirement <= homeAirport.runwayLength)
      .filter(model => if (allowedFamilies.isEmpty) true else allowedFamilies.contains(model.family))
      .filter(model => maxLifespan.forall(model.lifespan <= _))
      .filter(model => maxSpeed.forall(model.speed < _))
    val countryModels = eligibleModels.filter(_.countryCode == homeAirport.countryCode)
    val currentCycle = CycleSource.loadCycle()
    val selectedModel =
      if (eligibleModels.isEmpty) {
        None
      } else {
        if (!countryModels.isEmpty) {
          Some(countryModels(random.nextInt(countryModels.length)))
        } else {
          Some(eligibleModels(random.nextInt(eligibleModels.length)))
        }
      }
    selectedModel match {
      case Some(model) =>
        val amount = (budget / model.price).toInt
        val age = (Airplane.MAX_CONDITION - condition) / (Airplane.MAX_CONDITION.toDouble / model.lifespan)
        val constructedCycle = Math.max(0, currentCycle - age.toInt)
        (0 until amount).map(_ => Airplane(model, airline, constructedCycle, constructedCycle, condition, depreciationRate = 0, value = (model.price * condition / Airplane.MAX_CONDITION).toInt, home = homeAirport)).toList
      case None =>
        List.empty
    }
  }

  def generateProfiles(airline: Airline, airport: Airport): List[Profile] = {
    val difficulty = airport.rating.overallDifficulty
    val difficultyBonus = difficulty * BONUS_PER_DIFFICULTY_POINT
    val availableConfigs = ProfileConfigs.all.filter(_.isAvailable(airport)).sortBy(_.id)
    val profiles = ListBuffer[Profile]()
    availableConfigs.foreach { config =>
      val profileRandom = new Random(airport.id + config.id) // Independent random per profile for reproducibility
      val airplanesWithBudgets = config.planeConfigs.zip(config.difficultyToPlaneFractions).map { case (pc, frac) =>
        val adjustedBudget = pc.budget + (difficultyBonus * frac).toLong
        val airplanes = generateAirplanes(adjustedBudget, airport, pc.condition, airline, profileRandom, pc.allowedFamilies, pc.maxLifespan, pc.maxSpeed)
        (airplanes, adjustedBudget)
      }
      val airplanes = airplanesWithBudgets.flatMap(_._1)
      if (config.planeConfigs.isEmpty || airplanes.nonEmpty) {
        val planeCost = airplanes.map(_.value.toLong).sum
        val totalBudget = airplanesWithBudgets.map(_._2).sum
        val spentFull = airplanes.map(a => (a.value.toLong * Airplane.MAX_CONDITION.toLong) / a.condition.toLong.round).sum
        val unspent = totalBudget - spentFull
        val unspentToCash = (unspent * config.unspentToCashFraction).toLong
        val difficultyToCash = (difficultyBonus * config.difficultyToCashFraction).toLong
        val cash = config.cashBase + difficultyToCash + unspentToCash
        val currentCycle = CycleSource.loadCycle()
        val loans = config.loanConfigs.map { lc =>
          val principal = if (lc.isBasedOnPlaneCost) (planeCost * lc.principalFraction).toLong else lc.principalFixed
          Loan(airlineId = airline.id, principal = principal, annualRate = BigDecimal(lc.rate), creationCycle = currentCycle, lastPaymentCycle = currentCycle, term = lc.term)
        }
        val profile = Profile(
          name = config.name,
          description = config.description,
          cash = cash,
          airport = airport,
          reputation = config.reputation,
          airplanes = airplanes,
          loans = loans
        )
        profiles.append(profile)
      }
    }
    profiles.toList
  }

  def getProfiles(airlineId: Int, airportId: Int) = AuthenticatedAirline(airlineId) { request =>
    request.user.getHeadQuarter() match {
      case Some(headquarters) =>
        BadRequest("Cannot select profile with active HQ")
      case None =>
        Ok(Json.toJson(generateProfiles(request.user, AirportCache.getAirport(airportId, true).get)))
    }
  }

  private[this] val buildHqWithProfileLock = new Object()
  def buildHqWithProfile(airlineId: Int, airportId: Int, profileId: Int) = AuthenticatedAirline(airlineId) { request =>
    val airline = request.user
    buildHqWithProfileLock.synchronized {
      if (!airline.isInitialized) {
        val airport = AirportCache.getAirport(airportId, true).get
        val profiles = generateProfiles(airline, airport)
        if (profileId >= 0 && profileId < profiles.length) {
          val profile = profiles(profileId)
          val config = ProfileConfigs.all.find(_.id == profileId).get
          val base = AirlineBase(airline, airport, airport.countryCode, 1, CycleSource.loadCycle(), true)
          AirlineSource.saveAirlineBase(base)
          airline.setCountryCode(airport.countryCode)
          airline.setReputation(profile.reputation)
          airline.setBalance(profile.cash)
          airline.setCurrentServiceQuality(config.currentServiceQuality)
          AirportSource.updateAirlineAppeal(airportId, airlineId, AirlineAppeal(loyalty = 0))
          profile.airplanes.foreach(_.assignDefaultConfiguration())
          AirplaneSource.saveAirplanes(profile.airplanes)
          profile.loans.foreach { loan =>
            BankSource.saveLoan(loan)
          }
          airline.setInitialized(true)
          AirlineSource.saveAirlineInfo(airline, true)
          val updatedAirline = AirlineSource.loadAirlineById(airlineId, true)
          Ok(Json.toJson(updatedAirline))
        } else {
          BadRequest(s"Invalid profileId: $profileId")
        }
      } else {
        BadRequest(s"${request.user} was already initialized")
      }
    }
  }
}