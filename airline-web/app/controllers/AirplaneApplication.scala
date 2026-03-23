package controllers

import scala.math.BigDecimal.int2bigDecimal
import com.patson.data.{AirlineSource, AirplaneSource, CashFlowSource, CountrySource, CycleSource, LinkSource}
import com.patson.data.airplane.ModelSource
import com.patson.model.airplane.{Model, _}
import com.patson.model._
import com.patson.ChronologyConverter
import com.patson.OrderQueueSimulation
import play.api.libs.json.{JsArray, JsBoolean, JsNumber, JsObject, JsString, JsValue, Json, Writes}
import play.api.mvc._

import scala.collection.mutable.ListBuffer
import controllers.AuthenticationObject.AuthenticatedAirline
import com.patson.data.airplane.OrderQueueSource
import com.patson.model.AirlineTransaction
import com.patson.model.AirlineCashFlow
import com.patson.model.CashFlowType
import com.patson.model.AirlineCashFlowItem
import com.patson.model.airplane.Model.Category
import com.patson.util.{AirplaneOwnershipCache, CountryCache}

import javax.inject.Inject
import scala.collection.{MapView, mutable}


class AirplaneApplication @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  implicit object LinkAssignmentWrites extends Writes[LinkAssignments] {
    def writes(linkAssignments: LinkAssignments) : JsValue = {
      var result = Json.arr()
      linkAssignments.assignments.foreach {
        case(linkId, assignment) =>
          val link = LinkSource.loadFlightLinkById(linkId, LinkSource.SIMPLE_LOAD).getOrElse(Link.fromId(linkId))
          result = result.append(Json.obj("link" -> Json.toJson(link), "frequency" -> assignment.frequency))
      }
      result
    }
  }
  
  implicit object AirplaneWithAssignedLinkWrites extends Writes[(Airplane, LinkAssignments)] {
    def writes(airplaneWithAssignedLink : (Airplane, LinkAssignments)): JsValue = {
      val airplane = airplaneWithAssignedLink._1
      val jsObject = Json.toJson(airplane).asInstanceOf[JsObject]
      jsObject + ("links" -> Json.toJson(airplaneWithAssignedLink._2))
    }
  }

  implicit object AirplaneModelWithDiscountsWrites extends Writes[ModelWithDiscounts] {
    def writes(airplaneModelWithDiscounts : ModelWithDiscounts): JsValue = {
      if (airplaneModelWithDiscounts.discounts.isEmpty) {
        Json.toJson(airplaneModelWithDiscounts.originalModel)
      } else {
        val discountedModel = airplaneModelWithDiscounts.originalModel.applyDiscount(airplaneModelWithDiscounts.discounts)
        var result = Json.toJson(discountedModel).asInstanceOf[JsObject]
        if (discountedModel.price != airplaneModelWithDiscounts.originalModel.price) {
          result = result + ("originalPrice" -> JsNumber(airplaneModelWithDiscounts.originalModel.price))
        }
        if (discountedModel.constructionTime != airplaneModelWithDiscounts.originalModel.constructionTime) {
          result = result + ("originalConstructionTime" -> JsNumber(airplaneModelWithDiscounts.originalModel.constructionTime))
        }

        var discountsJson = Json.obj()
        airplaneModelWithDiscounts.discounts.groupBy(_.discountType).foreach {
          case (discountType, discounts) =>
            var discountsByTypeJson = Json.arr()
            discounts.foreach { discount =>
              discountsByTypeJson = discountsByTypeJson.append(Json.obj("discountDescription" -> discount.description, "discountPercentage" -> (discount.discount * 100).toInt))
            }
            val typeLabel = discountType.toString.toLowerCase
            discountsJson = discountsJson + (typeLabel -> discountsByTypeJson)
        }
        result = result + ("discounts" -> discountsJson)
        result
      }
    }
  }

  case class ModelWithDiscounts(originalModel : Model, discounts : List[ModelDiscount])

  sealed case class AirplanesByModel(model : Model, assignedAirplanes : List[Airplane], availableAirplanes : List[Airplane], constructingAirplanes: List[Airplane])
  
  object AirplanesByModelWrites extends Writes[List[AirplanesByModel]] {
    def writes(airplanesByModelList: List[AirplanesByModel]): JsValue = {
      var result = Json.obj()
      airplanesByModelList.foreach { airplanesByModel =>
        val airplaneJson = Json.obj(
          ("assignedAirplanes" -> Json.toJson(airplanesByModel.assignedAirplanes)),
            ("availableAirplanes" -> Json.toJson(airplanesByModel.availableAirplanes)),
            ("constructingAirplanes" -> Json.toJson(airplanesByModel.constructingAirplanes)))
        result = result + (String.valueOf(airplanesByModel.model.id) -> airplaneJson)
      }
      result
    }
  }
  object AirplanesByModelSimpleWrites extends Writes[List[AirplanesByModel]] {
    def writes(airplanesByModelList: List[AirplanesByModel]): JsValue = {
      var result = Json.obj()
      airplanesByModelList.foreach { airplanesByModel =>
        val airplaneJson = Json.obj(
          ("assignedAirplanes" -> Json.toJson(airplanesByModel.assignedAirplanes)(SimpleAirplanesWrites)),
          ("availableAirplanes" -> Json.toJson(airplanesByModel.availableAirplanes)(SimpleAirplanesWrites)),
          ("constructingAirplanes" -> Json.toJson(airplanesByModel.constructingAirplanes)(SimpleAirplanesWrites)))
        result = result + (String.valueOf(airplanesByModel.model.id) -> airplaneJson)
      }
      result
    }
  }

  object SimpleAirplanesWrites extends Writes[List[Airplane]] {
    override def writes(airplanes: List[Airplane]): JsValue = {
      var result = Json.arr()
      airplanes.foreach { airplane =>
        result = result.append(Json.toJson(airplane)(SimpleAirplaneWrite))
      }
      result
    }
  }

  def getAirplaneModels() = Action {
    Ok(Json.toJson(allAirplaneModels))
  }

  val MODEL_TOP_N = 10
  def getAirplaneModelStatsByAirline(airlineId : Int, modelId : Int) = AuthenticatedAirline(airlineId) { request =>
    Ok(getAirplaneModelStatsJson(modelId))
  }

  def getAirplaneModelStats(modelId : Int) = Action {
    Ok(getAirplaneModelStatsJson(modelId))
  }

  def getAirplaneModelStatsJson(modelId : Int) = {
    val airplanes = AirplaneSource.loadAirplanesCriteria(List(("a.model", modelId)))

    var result = Json.obj("total" -> airplanes.length)
    var topAirlinesJson = Json.arr()
    val airplanesCountByOwnerId : Map[Int, Int] = airplanes.filter(!_.isSold).groupBy(_.owner).view.map {
      case(airline, airplanes) => (airline.id, airplanes.length)
    }.toMap
    airplanesCountByOwnerId.toList.sortBy(_._2).reverse.take(MODEL_TOP_N).foreach {
      case (airlineId, airplaneCount) =>
        val airline = AirlineSource.loadAirlineById(airlineId)
        topAirlinesJson = topAirlinesJson.append(Json.obj("airline" -> Json.toJson(airline), "airplaneCount" -> airplaneCount))
    }
    result = result + ("topAirlines" -> topAirlinesJson)
    result
  }
  
  def getAirplaneModelsByAirline(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    val originalModels = ModelSource.loadAllModels()
    val originalModelsById = originalModels.map(model => (model.id, model)).toMap
    val currentCycle = CycleSource.loadCycle()
    val discountsByModelId = ModelDiscount.getAllCombinedDiscountsByAirlineId(airlineId, currentCycle)

    val discountedModels = originalModels.map { originalModel =>
      discountsByModelId.get(originalModel.id) match {
        case Some(discounts) if discounts.nonEmpty => originalModel.applyDiscount(discounts)
        case _ => originalModel
      }
    }

    val discountedModelWithRejections : Map[Model, Option[String]] = getRejections(discountedModels, request.user)

    val circulationByModelId : Map[Int, Int] =
      AirplaneSource.loadAirplanesCriteria(List(("is_sold", false)))
        .groupBy(_.model.id)
        .view.mapValues(_.size)
        .toMap

    var result = Json.arr()
    discountedModelWithRejections.toList.foreach {
      case(discountedModel, rejectionOption) =>
        val originalModel = originalModelsById(discountedModel.id)

        var modelJson =
          discountsByModelId.get(originalModel.id) match {
            case Some(discounts) if discounts.nonEmpty =>
              Json.toJson(ModelWithDiscounts(originalModel, discounts)).asInstanceOf[JsObject]
            case _ =>
              Json.toJson(originalModel).asInstanceOf[JsObject]
          }

        rejectionOption match {
          case Some(rejection) => modelJson = modelJson + ("rejection" -> JsString(rejection))
          case None => //
        }

        modelJson = modelJson + ("releaseCycle" -> JsNumber(originalModel.availabilityCycle))
        modelJson = modelJson + ("releaseYear"  -> JsNumber(originalModel.introYear))
        modelJson = modelJson + ("releaseDate"  -> JsString(s"Week ${originalModel.introWeek}, ${originalModel.introYear}"))
        modelJson = modelJson + ("totalInUse"   -> JsNumber(circulationByModelId.getOrElse(originalModel.id, 0).toLong))

        result = result.append(modelJson)
    }

    Ok(result)
  }
  
  def getRejections(models : List[Model], airline : Airline) : Map[Model, Option[String]] = {
    val allManufacturingCountries = models.map(_.countryCode).toSet
    val countryRelations : Map[String, AirlineCountryRelationship] = allManufacturingCountries.map { countryCode =>
      (countryCode, AirlineCountryRelationship.getAirlineCountryRelationship(countryCode, airline))
    }.toMap
    val ownedModels = AirplaneOwnershipCache.getOwnership(airline.id).map(_.model).toSet
    val currentCycle = CycleSource.loadCycle()

    models.map { model =>
      (model, getRejection(model, 1, countryRelations(model.countryCode), ownedModels, airline, currentCycle))
    }.toMap
  }
  
  def getRejection(model: Model, quantity : Int, airline : Airline) : Option[String] = {
    val relationship = AirlineCountryRelationship.getAirlineCountryRelationship(model.countryCode, airline)
    val ownedModels = AirplaneOwnershipCache.getOwnership(airline.id).map(_.model).toSet
    val currentCycle = CycleSource.loadCycle()
    getRejection(model, quantity, relationship, ownedModels, airline, currentCycle)
  }
  
  def getRejection(model: Model, quantity : Int, relationship : AirlineCountryRelationship, ownedModels : Set[Model], airline : Airline, currentCycle: Int) : Option[String] = {
    if (currentCycle < model.availabilityCycle) {
      return Some("This aircraft model has not yet been released")
    }  

    if (airline.getHeadQuarter().isEmpty) { 
      return Some("Must build HQs before purchasing any airplanes")
    }

    if (!model.purchasableWithRelationship(relationship.relationship)) {
      return Some(s"The manufacturer refuses to sell " + model.name + s" to your airline until your relationship with ${CountryCache.getCountry(model.countryCode).get.name} is improved to at least ${Model.BUY_RELATIONSHIP_THRESHOLD}")
    }

    val ownedModelFamilies = ownedModels.map(_.family)

    if (!ownedModelFamilies.contains(model.family) && ownedModelFamilies.size >= airline.airlineGrade.getModelFamilyLimit) {
      val familyToken = if (ownedModelFamilies.size <= 1) "family" else "families"
      return Some("Can only own up to " + airline.airlineGrade.getModelFamilyLimit + " different airplane " + familyToken + " at current airline grade")
    }

    // Balance check uses the already-discounted model price passed in by the caller
    val cost: Long = model.price.toLong * quantity
    if (cost > airline.getBalance()) {
      return Some("Not enough cash to purchase this airplane model")
    }
    
    return None
  }
  
  def getUsedRejections(usedAirplanes : List[Airplane], model : Model, airline : Airline) : Map[Airplane, String] = {
    if (airline.getHeadQuarter().isEmpty) {
      return usedAirplanes.map((_, "Must build HQs before purchasing any airplanes")).toMap
    }

    val countryRelationship = airline.getCountryCode() match {
      case Some(homeCountry) => CountrySource.getCountryMutualRelationship(homeCountry, model.countryCode)
      case None => 0
    }

    val relationship = AirlineCountryRelationship.getAirlineCountryRelationship(model.countryCode, airline)
    if (!model.purchasableWithRelationship(relationship.relationship)) {
      val rejection = s"Cannot buy used airplane of " + model.name + s" until your relationship with ${CountryCache.getCountry(model.countryCode).get.name} is improved to at least ${Model.BUY_RELATIONSHIP_THRESHOLD}"
      return usedAirplanes.map((_, rejection)).toMap
    }
    
    val ownedModels = AirplaneOwnershipCache.getOwnership(airline.id).map(_.model).toSet
    val ownedModelFamilies = ownedModels.map(_.family)
    if (!ownedModelFamilies.contains(model.family) && ownedModelFamilies.size >= airline.airlineGrade.getModelFamilyLimit) {
      val familyToken = if (ownedModelFamilies.size <= 1) "family" else "families"
      val rejection = "Can only own up to " + airline.airlineGrade.getModelFamilyLimit + " different airplane " + familyToken + " at current airline grade"
      return usedAirplanes.map((_, rejection)).toMap
    }
    
    val rejections = scala.collection.mutable.Map[Airplane, String]()
    usedAirplanes.foreach { airplane =>
      if (airplane.dealerValue > airline.getBalance()) {
         rejections.put(airplane, "Not enough cash to purchase this airplane")  
      }
    }
    return rejections.toMap
  }

  def getRecentAndUpcomingReleases() = Action {
    val currentCycle = CycleSource.loadCycle()
    val allModels = ModelSource.loadAllModels()

    val modelsWithCycle = allModels.map { model =>
      (model, model.availabilityCycle)
    }.sortBy(_._2)

    val TWO_YEARS_IN_CYCLES = 2 * ChronologyConverter.cyclesPerYear

    val recent = modelsWithCycle
      .filter(_._2 <= currentCycle)
      .sortBy(-_._2)
      .take(5)
      .map { case (model, _) =>
        Json.obj(
          "name"        -> model.name,
          "releaseDate" -> s"Week ${model.introWeek}, ${model.introYear}"
        )
      }

    val upcoming = modelsWithCycle
      .filter { case (_, rc) => rc > currentCycle && rc <= currentCycle + TWO_YEARS_IN_CYCLES }
      .take(8)
      .map { case (model, _) =>
        Json.obj(
          "name"        -> model.name,
          "releaseDate" -> s"Week ${model.introWeek}, ${model.introYear}"
        )
      }

    Ok(Json.obj(
      "recentReleases"   -> recent,
      "upcomingReleases" -> upcoming,
      "currentCycle"     -> currentCycle
    ))
  }

 def getModelQueueInfo(airlineId: Int, modelId: Int) = AuthenticatedAirline(airlineId) { request =>
    val (totalOrders, yourOrders): (Int, Int) = OrderQueueSource.countPendingByModelAndAirline(modelId, airlineId)
    Ok(Json.obj(
      "totalOrders" -> totalOrders,
      "yourOrders"  -> yourOrders
    ))
  }

  def getOwnedAirplanes(airlineId : Int, simpleResult : Boolean, groupedResult : Boolean) = {
    getAirplanes(airlineId, None, simpleResult, groupedResult)
  }

  def getOwnedAirplanesWithModelId(airlineId : Int, modelId : Int) = {
    getAirplanes(airlineId, Some(modelId), simpleResult = false, groupedResult = true)
  }

  private def getAirplanes(airlineId : Int, modelIdOption : Option[Int], simpleResult : Boolean, groupedResult : Boolean) = AuthenticatedAirline(airlineId) {
    val queryCriteria = ListBuffer(("owner", airlineId), ("is_sold", false))
    modelIdOption.foreach { modelId =>
      queryCriteria.append(("a.model", modelId))
    }

    val ownedAirplanes: List[Airplane] = AirplaneSource.loadAirplanesCriteria(queryCriteria.toList)
    val linkAssignments = AirplaneSource.loadAirplaneLinkAssignmentsByOwner(airlineId)
    if (groupedResult) {
      val airplanesByModel: Map[Model, (List[Airplane], List[Airplane])] = ownedAirplanes.groupBy(_.model).view.mapValues {
        airplanes => airplanes.partition(airplane => linkAssignments.isDefinedAt(airplane.id) && airplane.isReady)
      }.toMap

      val airplanesByModelList = airplanesByModel.toList.map {
        case (model, (assignedAirplanes, freeAirplanes)) => AirplanesByModel(model, assignedAirplanes, availableAirplanes = freeAirplanes.filter(_.isReady), constructingAirplanes=freeAirplanes.filter(!_.isReady))
      }
      var result =
        if (simpleResult) {
          Json.toJson(airplanesByModelList)(AirplanesByModelSimpleWrites)
        } else {
          Json.toJson(airplanesByModelList)(AirplanesByModelWrites)
        }
      Ok(result)
    } else {
      val airplanesWithLink : List[(Airplane, LinkAssignments)]= ownedAirplanes.map { airplane =>
        (airplane, linkAssignments.getOrElse(airplane.id, LinkAssignments.empty))
      }
      Ok(Json.toJson(airplanesWithLink))
    }
  }

  def getAirlineOrderQueue(airlineId: Int) = AuthenticatedAirline(airlineId) { request =>
    val currentCycle = CycleSource.loadCycle()
    val rows = OrderQueueSource.loadPendingOrdersByAirline(airlineId)
 
    // Group by (modelId, homeAirportId)
    val grouped = rows.groupBy(r => (r.modelId, r.homeAirportId))
 
    val orders = grouped.flatMap { case ((modelId, homeAirportId), groupRows) =>
      ModelSource.loadModelById(modelId).map { model =>
        // Earliest row by (orderCycle, shuffleIndex) determines next delivery cycle
        val earliest = groupRows.minBy(r => (r.orderCycle, r.shuffleIndex))
        val nextDeliveryCycle = earliest.orderCycle + model.constructionTime
        val weeksUntilNext = math.max(0, nextDeliveryCycle - currentCycle)
 
        // Resolve airport code from airline bases
        val airlineObj = request.user
        val homeAirportCode = airlineObj.getBases()
          .find(_.airport.id == homeAirportId)
          .map(_.airport.iata)
          .getOrElse(homeAirportId.toString)
 
        Json.obj(
          "modelId"          -> modelId,        
          "modelName"        -> model.name,
          "quantity"         -> groupRows.size,
          "homeAirportCode"  -> homeAirportCode,
          "homeAirportId"    -> homeAirportId,
          "nextDeliveryCycle"-> nextDeliveryCycle,
          "weeksUntilNext"   -> weeksUntilNext
        )
      }
    }.toList.sortBy(o => (o \ "modelName").as[String])
 
    Ok(Json.obj("orders" -> orders))
  }

  def getUsedAirplanes(airlineId : Int, modelId : Int) = AuthenticatedAirline(airlineId) { request =>
      ModelSource.loadModelById(modelId) match {
        case Some(model) => 
          val usedAirplanes = AirplaneSource.loadAirplanesCriteria(List(("a.model", modelId), ("is_sold", true)))
          
          val rejections = getUsedRejections(usedAirplanes, model, request.user)
          var result = Json.arr()
          usedAirplanes.foreach { airplane =>
            var airplaneJson = Json.toJson(airplane).asInstanceOf[JsObject]
            if (rejections.contains(airplane)) {
              airplaneJson = airplaneJson + ("rejection" -> JsString(rejections(airplane)))
            }
            result = result :+ airplaneJson
          }
          Ok(result)
        case None => BadRequest("model not found")
      }
  }
  
  def buyUsedAirplane(airlineId : Int, airplaneId : Int, homeAirportId : Int, configurationId : Int) = AuthenticatedAirline(airlineId) { request =>
      this.synchronized {
        AirplaneSource.loadAirplaneById(airplaneId) match {
          case Some(airplane) =>
            val airline = request.user
            getUsedRejections(List(airplane), airplane.model, airline).get(airplane) match {
              case Some(rejection) => BadRequest(rejection)
              case None =>
                if (!airplane.isSold) {
                  BadRequest("Airplane is no longer for sale " + airlineId)
                } else {
                  val homeBase = request.user.getBases().find(_.airport.id == homeAirportId)
                  homeBase match {
                    case None =>
                      BadRequest(s"Home airport ID $homeAirportId is not valid")
                    case Some(homeBase) =>
                      val configuration: Option[AirplaneConfiguration] =
                        if (configurationId == -1) {
                          None
                        } else {
                          AirplaneSource.loadAirplaneConfigurationById(configurationId)
                        }

                      if (configuration.isDefined && (configuration.get.airline.id != airlineId || configuration.get.model.id != airplane.model.id)) {
                        BadRequest("Configuration is not owned by this airline/model")
                      } else {
                        val dealerValue = airplane.dealerValue
                        val actualValue = airplane.value
                        airplane.buyFromDealer(airline, CycleSource.loadCycle())
                        airplane.home = homeBase.airport
                        airplane.purchaseRate = 1
                        configuration.foreach { configuration =>
                          airplane.configuration = configuration
                        }

                        if (AirplaneSource.updateAirplanes(List(airplane)) == 1) {
                          val capitalGain = actualValue - dealerValue
                          AirlineSource.adjustAirlineBalance(airline.id, dealerValue * -1)
                          AirlineSource.saveTransaction(AirlineTransaction(airlineId = airline.id, transactionType = TransactionType.CAPITAL_GAIN, amount = capitalGain))
                          AirlineSource.saveCashFlowItem(AirlineCashFlowItem(airlineId, CashFlowType.BUY_AIRPLANE, dealerValue * -1))
                          Ok(Json.obj())
                        } else {
                          BadRequest("Failed to buy used airplane " + airlineId)
                        }
                      }
                  }
                }
            }

          case None => BadRequest("airplane not found")
        }
      }
  }
  
  def getAirplane(airlineId : Int, airplaneId : Int) = AuthenticatedAirline(airlineId) {
    AirplaneSource.loadAirplaneById(airplaneId) match {
      case Some(airplane) =>
        if (airplane.owner.id == airlineId) {
          val airplaneWithLinkAssignments : (Airplane, LinkAssignments) = (airplane, AirplaneSource.loadAirplaneLinkAssignmentsByAirplaneId(airplane.id))
          Ok(Json.toJson(airplaneWithLinkAssignments))
        } else {
          Forbidden
        }
      case None =>
        BadRequest("airplane not found")
    }
  }
  
  def sellAirplane(airlineId : Int, airplaneId : Int) = AuthenticatedAirline(airlineId) {
    AirplaneSource.loadAirplaneById(airplaneId) match {
      case Some(airplane) =>
        if (airplane.owner.id != airlineId || airplane.isSold) {
          Forbidden
        } else if (!airplane.isReady) {
          BadRequest("airplane is not yet constructed or is sold")
        } else {
          val linkAssignments = AirplaneSource.loadAirplaneLinkAssignmentsByAirplaneId(airplaneId)
          if (!linkAssignments.isEmpty) {
            BadRequest("airplane " + airplane + " still assigned to link " + linkAssignments)
          } else {
            val sellValue = Computation.calculateAirplaneSellValue(airplane)

            val updateCount =
              if (airplane.condition >= Airplane.BAD_CONDITION) {
                airplane.sellToDealer()
                AirplaneSource.updateAirplanes(List(airplane.copy()), true)
              } else {
                AirplaneSource.deleteAirplane(airplaneId, Some(airplane.version))
              }

            if (updateCount == 1) {
              AirlineSource.adjustAirlineBalance(airlineId, sellValue)
              AirlineSource.saveTransaction(AirlineTransaction(airlineId, TransactionType.CAPITAL_GAIN, sellValue - airplane.value))
              AirlineSource.saveCashFlowItem(AirlineCashFlowItem(airlineId, CashFlowType.SELL_AIRPLANE, sellValue))
              Ok(Json.toJson(airplane))
            } else {
              BadRequest("Update failed")
            }
          }
        }
      case None =>
        BadRequest("airplane not found")
    }
  }
  
  def replaceAirplane(airlineId : Int, airplaneId : Int) = AuthenticatedAirline(airlineId) { request =>
    AirplaneSource.loadAirplaneById(airplaneId) match {
      case Some(airplane) =>
        if (airplane.owner.id == airlineId) {
          val currentCycle = CycleSource.loadCycle()
          if (!airplane.isReady) {
            BadRequest("airplane is not yet constructed")
          } else if (airplane.purchasedCycle > (currentCycle - airplane.model.constructionTime)) {
            BadRequest("airplane is not yet ready to be replaced")
          } else {
            val sellValue = Computation.calculateAirplaneSellValue(airplane)
            val originalModel = airplane.model
            // Replacement pays current market rate — obsolescence applies, no bulk discount
            val model = originalModel.applyDiscount(ModelDiscount.getCombinedDiscountsByModelId(airlineId, originalModel.id, currentCycle))
            val replaceCost = model.price - sellValue
            val purchaseRate = model.price.toDouble / originalModel.price
            if (request.user.airlineInfo.balance < replaceCost) {
              BadRequest("Not enough money")
            } else {
              val replacingAirplane = airplane.copy(constructedCycle = currentCycle, purchasedCycle = currentCycle, condition = Airplane.MAX_CONDITION, value = originalModel.price, purchaseRate = purchaseRate)
              val updateCount = AirplaneSource.updateAirplanes(List(replacingAirplane), true)
              if (updateCount == 1) {
                AirlineSource.adjustAirlineBalance(airlineId, -1 * replaceCost)
                val sellAirplaneLoss = sellValue - airplane.value
                val discountAirplaneGain = originalModel.price - model.price
                AirlineSource.saveTransaction(AirlineTransaction(airlineId, TransactionType.CAPITAL_GAIN, sellAirplaneLoss + discountAirplaneGain))
                AirlineSource.saveCashFlowItem(AirlineCashFlowItem(airlineId, CashFlowType.SELL_AIRPLANE, sellValue))
                AirlineSource.saveCashFlowItem(AirlineCashFlowItem(airlineId, CashFlowType.BUY_AIRPLANE, model.price * -1))
                Ok(Json.toJson(airplane))
              } else {
                BadRequest("Something went wrong, try again!")
              }
            }
          }
        } else {
          Forbidden
        }
      case None =>
        BadRequest("airplane not found")
    }
  }
  
  def addAirplane(airlineId : Int, modelId : Int, quantity : Int, homeAirportId : Int, configurationId : Int) = AuthenticatedAirline(airlineId) { request =>
    ModelSource.loadModelById(modelId) match {
      case None =>
        BadRequest("unknown model or airline")
      case Some(originalModel) =>
        val airline = request.user
        val currentCycle = CycleSource.loadCycle()

        val baseDiscounts = ModelDiscount.getCombinedDiscountsByModelId(airlineId, modelId, currentCycle)
        val bulkDiscount  = ModelDiscount.computeBulkOrderDiscount(originalModel, quantity)
        val allDiscounts  = baseDiscounts ++ bulkDiscount.toList

        val discountedModel = originalModel.applyDiscount(allDiscounts)

        val homeBase = request.user.getBases().find(_.airport.id == homeAirportId)
        homeBase match {
          case None =>
            BadRequest(s"Home airport ID $homeAirportId is not valid")
          case Some(homeBase) =>
            val rejectionOption = getRejection(discountedModel, quantity, airline)
            if (rejectionOption.isDefined) {
              BadRequest(rejectionOption.get)
            } else {
              val totalCost : Long = discountedModel.price.toLong * quantity

              AirlineSource.adjustAirlineBalance(airlineId, -totalCost)
              AirlineSource.saveCashFlowItem(AirlineCashFlowItem(airlineId, CashFlowType.BUY_AIRPLANE, -totalCost))

              if (originalModel.price != discountedModel.price) {
                val discountGain = (originalModel.price - discountedModel.price).toLong * quantity
                AirlineSource.saveTransaction(AirlineTransaction(airlineId = airlineId, transactionType = TransactionType.CAPITAL_GAIN, amount = discountGain))
              }

              if (originalModel.constructionTime == 0) {
                // Immediate delivery — bypass queue entirely
                val airplanes = ListBuffer[Airplane]()
                for (_ <- 0 until quantity) {
                  airplanes.append(Airplane(
                    model            = discountedModel,
                    owner            = airline,
                    constructedCycle = currentCycle,
                    purchasedCycle   = currentCycle,
                    condition        = Airplane.MAX_CONDITION,
                    depreciationRate = 0,
                    value            = originalModel.price,
                    home             = homeBase.airport,
                    isReady          = true,
                    purchaseRate     = discountedModel.price.toDouble / originalModel.price
                  ))
                }
                
                airplanes.foreach(_.assignDefaultConfiguration())
                AirplaneSource.saveAirplanes(airplanes.toList)
                Ok(Json.obj(
                  "updateCount"     -> quantity,
                  "totalCost"       -> totalCost,
                  "discountedPrice" -> discountedModel.price,
                  "originalPrice"   -> originalModel.price
                ))
              } else {
                // Normal queue path
                OrderQueueSimulation.placeOrder(modelId, airlineId, quantity, currentCycle, homeBase.airport.id)
                Accepted(Json.obj(
                  "updateCount"                -> quantity,
                  "totalCost"                  -> totalCost,
                  "discountedPrice"            -> discountedModel.price,
                  "originalPrice"              -> originalModel.price,
                  "expectedDeliveryStartCycle" -> (currentCycle + discountedModel.constructionTime),
                  "constructionTime"           -> discountedModel.constructionTime,
                  "homeAirportId"              -> homeBase.airport.id
                ))
              }
            }
        }
    }
  }
  
  def swapAirplane(airlineId : Int, fromAirplaneId : Int, toAirplaneId : Int) = AuthenticatedAirline(airlineId) { request =>
    val fromAirplaneOption = AirplaneSource.loadAirplaneById(fromAirplaneId)
    val toAirplaneOption = AirplaneSource.loadAirplaneById(toAirplaneId)

    if (fromAirplaneOption.isDefined && toAirplaneOption.isDefined) {
      val fromAirplane = fromAirplaneOption.get
      val toAirplane = toAirplaneOption.get
      if (fromAirplane.owner.id == airlineId && toAirplane.owner.id == airlineId && fromAirplane.model.id == toAirplane.model.id) {
        val fromConstructedCycle = fromAirplane.constructedCycle
        val fromPurchaseCycle = fromAirplane.purchasedCycle
        val fromCondition = fromAirplane.condition
        val fromValue = fromAirplane.value
        val fromPurchaseRate = fromAirplane.purchaseRate

        val toConstructedCycle = toAirplane.constructedCycle
        val toPurchaseCycle = toAirplane.purchasedCycle
        val toCondition = toAirplane.condition
        val toValue = toAirplane.value
        val toPurchaseRate = toAirplane.purchaseRate

        val swappedFromAirplane = fromAirplane.copy(constructedCycle = toConstructedCycle, purchasedCycle = toPurchaseCycle, condition = toCondition, value = toValue, purchaseRate = toPurchaseRate)
        val swappedToAirplane = toAirplane.copy(constructedCycle = fromConstructedCycle, purchasedCycle = fromPurchaseCycle, condition = fromCondition, value = fromValue, purchaseRate = fromPurchaseRate)

        AirplaneSource.updateAirplanes(List(swappedFromAirplane, swappedToAirplane))
        LinkUtil.adjustLinksAfterAirplaneConfigurationChange(swappedFromAirplane.id)
        LinkUtil.adjustLinksAfterAirplaneConfigurationChange(swappedToAirplane.id)

        Ok(Json.toJson(fromAirplane))
      } else {
        Forbidden
      }
    } else {
        BadRequest("airplane not found")
    }
  }

  def updateAirplaneHome(airlineId : Int, airplaneId : Int, airportId: Int) = AuthenticatedAirline(airlineId) { request =>
    AirplaneSource.loadAirplaneById(airplaneId) match {
      case Some(airplane) =>
        if (airplane.owner.id != airlineId) {
          BadRequest(s"Cannot update Home on airplane $airplane as it is not owned by ${request.user.name}")
        } else {
          if (!AirplaneSource.loadAirplaneLinkAssignmentsByAirplaneId(airplane.id).isEmpty) {
            BadRequest(s"Cannot update Home on airplane $airplane as it has assigned links")
          } else {
            request.user.getBases().find(_.airport.id == airportId) match {
              case Some(base) =>
                airplane.home = base.airport
                AirplaneSource.updateAirplanesDetails(List(airplane))
                Ok(Json.toJson(airplane))
              case None =>
                BadRequest(s"Cannot update Home on airplane $airplaneId as base $airportId is not found")
            }
          }
        }
      case None => BadRequest(s"Cannot update Configuration on airplane $airplaneId as it is not found")
    }
  }

  def getMaintenanceFactor(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    val info = AirplaneOwnershipCache.getOwnershipInfo(airlineId)

    Ok(Json.obj("factor" -> AirplaneMaintenanceUtil.getMaintenanceFactor(airlineId),
      "baseFactor" -> AirplaneMaintenanceUtil.BASE_MAINTENANCE_FACTOR,
      "familyFactor" -> AirplaneMaintenanceUtil.PER_FAMILY_MAINTENANCE_FACTOR,
      "modelFactor" -> AirplaneMaintenanceUtil.PER_MODEL_MAINTENANCE_FACTOR,
      "families" -> Json.toJson(info.families.toList.sorted),
      "models" -> Json.toJson(info.models.map(_.name).toList.sorted),
    ))
  }
}