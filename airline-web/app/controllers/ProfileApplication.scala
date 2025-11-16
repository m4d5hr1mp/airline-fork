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

class ProfileApplication @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  implicit object ProfileWrites extends Writes[Profile] {
    def writes(profile: Profile): JsValue = {
      var result =Json.obj(
        "name" -> profile.name,
        "description" -> profile.description,
        "cash" -> profile.cash,
        "airplanes" -> profile.airplanes,
        "reputation" -> profile.reputation,
        "airportText" -> profile.airport.displayText)
      profile.loan.foreach { loan =>
        result = result + ("loan" -> Json.toJson(loan)(new LoanWrites(CycleSource.loadCycle())))
      }
      result
    }
  }

  val BASE_CAPITAL = 750_000_000
  val BONUS_PER_DIFFICULTY_POINT = 10_000_000

  def generateAirplanes(value : Int, capacityRange : scala.collection.immutable.Range, homeAirport : Airport, condition : Double, airline : Airline, random : Random) : List[Airplane] =  {
    val eligibleModels = allAirplaneModels.filter(model => capacityRange.contains(model.capacity))
      .filter(model => model.purchasableWithRelationship(allCountryRelationships.getOrElse((homeAirport.countryCode, model.countryCode), 0)))
      .filter(model => model.price * condition / Airplane.MAX_CONDITION <= value / 3)
      .filter(model => model.runwayRequirement <= homeAirport.runwayLength)
    val countryModels = eligibleModels.filter(_.countryCode == homeAirport.countryCode)
    val currentCycle = CycleSource.loadCycle()

    val selectedModel =
      if (eligibleModels.isEmpty) {
        None
      } else {
        if (!countryModels.isEmpty) { //always go for airplanes from this country first
          Some(countryModels(random.nextInt(countryModels.length)))
        } else {
          Some(eligibleModels(random.nextInt(eligibleModels.length)))
        }
      }
    selectedModel match {
      case Some(model) =>
        val amount = value / model.price
        val age = (Airplane.MAX_CONDITION - condition) / (Airplane.MAX_CONDITION.toDouble / model.lifespan)  //not really that useful, just to fake a more reasonable number
        val constructedCycle = Math.max(0, currentCycle - age.toInt)
        (0 until amount).map(_ => Airplane(model, airline, constructedCycle, constructedCycle, condition, depreciationRate = 0, value = (model.price * condition / Airplane.MAX_CONDITION).toInt, home = homeAirport)).toList
      case None =>
        List.empty
    }
  }

  val BASE_INTEREST_RATE = 0.1 //10%

  def generateProfiles(airline : Airline, airport : Airport) : List[Profile] = {
    val difficulty = airport.rating.overallDifficulty
    val capital = BASE_CAPITAL + difficulty * BONUS_PER_DIFFICULTY_POINT
    val profiles = ListBuffer[Profile]()
    val random = new Random(airport.id)
    val currentCycle = CycleSource.loadCycle()
    val condition = 95.0 // High condition for young, premium starter fleet
    val planeBudgetMultiplier = 2.0
    val cashMultiplier = 3.0
    val planeBudget = (capital * planeBudgetMultiplier).toInt
    val valueSmall = planeBudget / 2
    val valueBig = planeBudget - valueSmall
    val smallRange = 76 to 134
    val bigRange = 135 to 212

    // Filter eligible models for small category (76-134 pax)
    val eligibleSmall = allAirplaneModels.filter(model => smallRange.contains(model.capacity))
      .filter(model => model.purchasableWithRelationship(allCountryRelationships.getOrElse((airport.countryCode, model.countryCode), 0)))
      .filter(model => model.price * condition / Airplane.MAX_CONDITION <= valueSmall / 3)
      .filter(model => model.runwayRequirement <= airport.runwayLength)
    val countrySmall = eligibleSmall.filter(_.countryCode == airport.countryCode)
    val selectedSmallModel = if (eligibleSmall.nonEmpty) {
      val candidates = if (countrySmall.nonEmpty) countrySmall else eligibleSmall
      Some(candidates.maxBy(_.capacity)) // Select biggest (max capacity) model
    } else None

    // Generate small airplanes
    val smallAirplanes = selectedSmallModel match {
      case Some(model) =>
        val amount = valueSmall / model.price
        val age = (Airplane.MAX_CONDITION - condition) / (Airplane.MAX_CONDITION.toDouble / model.lifespan)
        val constructedCycle = Math.max(0, currentCycle - age.toInt)
        (0 until amount).map { _ =>
          Airplane(model, airline, constructedCycle, constructedCycle, condition, depreciationRate = 0,
            value = (model.price * condition / Airplane.MAX_CONDITION).toInt, home = airport)
        }.toList
      case None => List.empty
    }

    // Filter eligible models for big category (135-212 pax)
    val eligibleBig = allAirplaneModels.filter(model => bigRange.contains(model.capacity))
      .filter(model => model.purchasableWithRelationship(allCountryRelationships.getOrElse((airport.countryCode, model.countryCode), 0)))
      .filter(model => model.price * condition / Airplane.MAX_CONDITION <= valueBig / 3)
      .filter(model => model.runwayRequirement <= airport.runwayLength)
    val countryBig = eligibleBig.filter(_.countryCode == airport.countryCode)
    val selectedBigModel = if (eligibleBig.nonEmpty) {
      val candidates = if (countryBig.nonEmpty) countryBig else eligibleBig
      Some(candidates(random.nextInt(candidates.length))) // Select random model
    } else None

    // Generate big airplanes
    val bigAirplanes = selectedBigModel match {
      case Some(model) =>
        val amount = valueBig / model.price
        val age = (Airplane.MAX_CONDITION - condition) / (Airplane.MAX_CONDITION.toDouble / model.lifespan)
        val constructedCycle = Math.max(0, currentCycle - age.toInt)
        (0 until amount).map { _ =>
          Airplane(model, airline, constructedCycle, constructedCycle, condition, depreciationRate = 0,
            value = (model.price * condition / Airplane.MAX_CONDITION).toInt, home = airport)
        }.toList
      case None => List.empty
    }

    val airplanes = smallAirplanes ++ bigAirplanes
    val totalPlaneValue = airplanes.map(_.value).sum
    val entrepreneurialCash = (capital * cashMultiplier).toInt - totalPlaneValue + (difficulty * BONUS_PER_DIFFICULTY_POINT / 2).toInt

    // Updated "Entrepreneurial spirit" profile with custom fleet, reputation 100, no loan
    val entrepreneurialProfile = Profile(
      name = "Entrepreneurial spirit",
      description = "You channel your entrepreneurial spirit into a prime starter fleet of efficient small and medium aircraft, exceptional reputation, and generous cash reserves. Plan carefully but make bold moves to thrive in this brave new world. Recommended for new players.",
      cash = entrepreneurialCash,
      airport = airport,
      reputation = 100,
      airplanes = airplanes
      // No loan, as per original
    )
    profiles.append(entrepreneurialProfile)

    // Existing "A humble beginning" profile (unchanged)
    val humbleAirplanes = generateAirplanes(capital, (90 to 180), airport, 90, airline, random)
    if (!humbleAirplanes.isEmpty) {
      val humbleAirplaneProfile = Profile(
        name = "A humble beginning",
        description = "A newly acquired airline with a modest aircraft fleet of young age. Grow this humble airline into the most powerful and respected brand in the aviation world!",
        cash = (capital * 1.5).toInt - humbleAirplanes.map(_.value).sum + difficulty * BONUS_PER_DIFFICULTY_POINT / 2,
        airport = airport,
        reputation = 10,
        airplanes = humbleAirplanes,
        loan = Some(Bank.getLoanOptions((capital * 1).toInt, BASE_INTEREST_RATE, CycleSource.loadCycle()).last.copy(airlineId = airline.id)))
      profiles.append(humbleAirplaneProfile)
    }

    // Existing "Revival of past glory" profile (unchanged)
    val largeAirplanes = generateAirplanes(capital * 3, (70 to 200), airport, 70, airline, random)
    if (!largeAirplanes.isEmpty) {
      val largeAirplaneProfile = Profile(
        name = "Revival of past glory",
        description = "An airline that has previously over-expanded by mismanagement of now retired CEO. It is left with some aging airplanes and heavy debt. Can you turn this airline around?",
        cash = (capital * 4).toInt - largeAirplanes.map(_.value).sum + difficulty * BONUS_PER_DIFFICULTY_POINT,
        airport = airport,
        reputation = 25,
        airplanes = largeAirplanes,
        loan = Some(Bank.getLoanOptions((capital * 3.5).toInt, BASE_INTEREST_RATE, CycleSource.loadCycle()).last.copy(airlineId = airline.id)))
      profiles.append(largeAirplaneProfile)
    }

    profiles.toList
  }

  def getProfiles(airlineId : Int, airportId : Int) = AuthenticatedAirline(airlineId) { request =>
    request.user.getHeadQuarter() match {
      case Some(headquarters) =>
        BadRequest("Cannot select profile with active HQ")
      case None =>
        Ok(Json.toJson(generateProfiles(request.user, AirportCache.getAirport(airportId, true).get)))
    }

  }

  private[this] val buildHqWithProfileLock = new Object()
  def buildHqWithProfile(airlineId : Int, airportId : Int, profileId : Int) = AuthenticatedAirline(airlineId) { request =>
    val airline = request.user
    buildHqWithProfileLock.synchronized {
      if (!airline.isInitialized) {
        val airport = AirportCache.getAirport(airportId, true).get
        val profile = generateProfiles(airline, airport)(profileId)

        val base = AirlineBase(airline, airport, airport.countryCode, 1, CycleSource.loadCycle(), true)
        AirlineSource.saveAirlineBase(base)
        airline.setCountryCode(airport.countryCode)
        airline.setReputation(profile.reputation)
        airline.setBalance(profile.cash)
        AirportSource.updateAirlineAppeal(airportId, airlineId, AirlineAppeal(loyalty = 0))

        profile.airplanes.foreach(_.assignDefaultConfiguration())
        AirplaneSource.saveAirplanes(profile.airplanes)

        profile.loan.foreach { loan =>
          BankSource.saveLoan(loan)
        }

        airline.setInitialized(true)
        AirlineSource.saveAirlineInfo(airline, true)
        val updatedAirline = AirlineSource.loadAirlineById(airlineId, true)

        Ok(Json.toJson(updatedAirline))
      } else {
        BadRequest(s"${request.user} was already initialized")
      }
    }
  }
}
