package com.patson.init

import scala.collection.mutable
import com.patson.data._
import com.patson.data.Constants._
import com.patson.model._
import com.patson.model.airplane._

import java.util.Calendar
import com.patson.Authentication

import scala.util.Random
import com.patson.DemandGenerator
import com.patson.data.airplane._

import com.patson.util.LogoGenerator
import java.util.concurrent.ThreadLocalRandom
import scala.concurrent.Await
import scala.concurrent.duration.Duration


object AirlineGenerator extends App {
  mainFlow

  private lazy val allModels = ModelSource.loadAllModels()
  private lazy val airports = AirportSource.loadAllAirports(true).sortBy(_.power)

  def mainFlow() = {
    deleteAirlines()

    GeneratedAirlinesConfigs.airlineDeclarations.foreach(processDeclaredAirline)

    println("DONE Creating airlines")
    Await.result(actorSystem.terminate(), Duration.Inf)
  }

  private def processDeclaredAirline(config: DeclaredAirline): Unit = {
    val bufferOfAirplanes = mutable.Map[Model, (Airplane, Int)]()   // fresh buffer per airline

    val hqBase = config.bases.find(_.isHq).getOrElse(config.bases.head)
    val hqAirport = airports.find(_.iata == hqBase.iata)
      .getOrElse(throw new IllegalArgumentException(s"HQ airport ${hqBase.iata} not found"))

    val user = createUser(config.username)
    val airline = createAirline(config.name, hqAirport, config.targetServiceQuality, config.initialBalance)

    println(s"generating ${config.name} at ${hqAirport.iata}")

    AirlineSource.saveAirlines(List(airline))
    AirlineSource.saveLogo(airline.id, LogoGenerator.generateRandomLogo())
    UserSource.setUserAirline(user, airline)
    AirlineSource.saveAirlineBase(AirlineBase(airline, hqAirport, hqAirport.countryCode, hqBase.scale, 1, hqBase.isHq))
    AirlineSource.saveAirplaneRenewal(airline.id, 50)

    // additional bases
    config.bases.filterNot(_.isHq).foreach { b =>
      val airport = airports.find(_.iata == b.iata).get
      AirlineSource.saveAirlineBase(AirlineBase(airline, airport, airport.countryCode, b.scale, 1, false))
    }

    // generate explicit routes
    config.bases.foreach { b =>
      val fromAirport = airports.find(_.iata == b.iata).get
      b.routeGroups.foreach { group =>
        val model = allModels.find(_.name == group.modelName)
          .getOrElse(throw new IllegalArgumentException(s"Model ${group.modelName} not found"))

        group.toIatas.foreach { toIata =>
          val toAirport = airports.find(_.iata == toIata).get
          createExplicitLink(fromAirport, toAirport, airline, model, group.frequency, group.rawQuality, bufferOfAirplanes)
        }
      }
    }
  }

  private def createExplicitLink(
    fromAirport: Airport,
    toAirport: Airport,
    airline: Airline,
    model: Model,
    frequency: Int,
    rawQuality: Int,
    bufferOfAirplanes: mutable.Map[Model, (Airplane, Int)]
  ): Unit = {
    if (frequency <= 0) return

    val distance = Computation.calculateDistance(fromAirport, toAirport)
    if (model.range < distance) {
      println(s"Skipping ${fromAirport.iata} → ${toAirport.iata} (${model.name} range too short)")
      return
    }

    val maxFrequencyPerAirplane = Computation.calculateMaxFrequency(model, distance)
    val airplanesRequired = math.max(1, (frequency + maxFrequencyPerAirplane - 1) / maxFrequencyPerAirplane)

    val assignedAirplanes = createAndAssignAirplanes(
      model, airline, fromAirport, frequency, distance, airplanesRequired, maxFrequencyPerAirplane, bufferOfAirplanes
    )

    val capacity = calculateTotalCapacity(assignedAirplanes)

    val flightType = Computation.getFlightType(fromAirport, toAirport, distance)
    val price = Pricing.computeStandardPrice(distance, flightType, ECONOMY)
    val duration = Computation.calculateDuration(model, distance)

    val newLink = Link(
      fromAirport,
      toAirport,
      airline,
      LinkClassValues.getInstance(price),
      distance,
      LinkClassValues.getInstance(capacity),
      rawQuality = rawQuality,
      duration = duration,
      frequency = frequency,
      flightType = flightType
    )

    newLink.setAssignedAirplanes(assignedAirplanes)
    LinkSource.saveLinks(List(newLink))
  }

  private def createAndAssignAirplanes(
    model: Model,
    airline: Airline,
    homeAirport: Airport,
    frequency: Int,
    distance: Int,
    airplanesRequired: Int,
    maxFrequencyPerAirplane: Int,
    bufferOfAirplanes: mutable.Map[Model, (Airplane, Int)]
  ): Map[Airplane, LinkAssignment] = {
    val assignedAirplanes = mutable.Map[Airplane, LinkAssignment]()
    val flightMinutesRequired = Computation.calculateFlightMinutesRequired(model, distance)
    var remainingFrequency = frequency

    for (_ <- 0 until airplanesRequired if remainingFrequency > 0) {
      bufferOfAirplanes.get(model) match {
        case Some((usedAirplane, remainingMinutes)) if remainingMinutes >= flightMinutesRequired =>
          val freqThis = math.min(remainingFrequency, remainingMinutes / flightMinutesRequired)
          val minutesThis = freqThis * flightMinutesRequired
          val newRemaining = remainingMinutes - minutesThis

          if (newRemaining > 120) {
            bufferOfAirplanes(model) = (usedAirplane, newRemaining)
          } else {
            bufferOfAirplanes.remove(model)
          }
          assignedAirplanes.put(usedAirplane, LinkAssignment(freqThis, minutesThis))
          remainingFrequency -= freqThis

        case _ =>
          val newAirplane = createAirplane(model, airline, homeAirport)
          val freqThis = math.min(remainingFrequency, maxFrequencyPerAirplane)
          val minutesThis = freqThis * flightMinutesRequired

          if (Airplane.MAX_FLIGHT_MINUTES - minutesThis > 120) {
            bufferOfAirplanes(model) = (newAirplane, Airplane.MAX_FLIGHT_MINUTES - minutesThis)
          }
          assignedAirplanes.put(newAirplane, LinkAssignment(freqThis, minutesThis))
          remainingFrequency -= freqThis
      }
    }
    assignedAirplanes.toMap
  }

  private def createAirplane(model: Model, airline: Airline, homeAirport: Airport): Airplane = {
    val airplane = Airplane(
      model = model,
      owner = airline,
      constructedCycle = 0,
      purchasedCycle = 0,
      condition = Airplane.MAX_CONDITION,
      depreciationRate = 0,
      value = model.price,
      home = homeAirport,
      configuration = AirplaneConfiguration.empty
    )
    airplane.assignDefaultConfiguration()
    AirplaneSource.saveAirplanes(List(airplane))
    airplane
  }

  private def calculateTotalCapacity(assignedAirplanes: Map[Airplane, LinkAssignment]): Int = {
    assignedAirplanes.map { case (airplane, assignment) =>
      airplane.configuration.economyVal * assignment.frequency
    }.sum
  }

  def deleteAirlines(): Unit = {
    println("Deleting airlines...")
    UserSource.deleteGeneratedUsers(0)   // change to () or remove the argument if your version complains
  }

  private def createUser(userName: String): User = {
    val user = User(userName = userName, email = "", Calendar.getInstance, Calendar.getInstance, UserStatus.ACTIVE, level = 0, None, List.empty)
    UserSource.saveUser(user)
    Authentication.createUserSecret(userName, "1234")
    user
  }

  private def createAirline(name: String, hqAirport: Airport, targetServiceQuality: Int, initialBalance: Long): Airline = {
    val airline = Airline(name, isGenerated = true)
    airline.setBalance(initialBalance)
    airline.setMaintenanceQuality(100)
    airline.setTargetServiceQuality(targetServiceQuality)
    airline.setCountryCode(hqAirport.countryCode)
    airline.setAirlineCode(airline.getDefaultAirlineCode())
    airline
  }
}