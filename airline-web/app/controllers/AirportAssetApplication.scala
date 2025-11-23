package controllers

import com.patson.data.{AirlineSource, AirportAssetSource, CycleSource}
import com.patson.model.AirportAssetType.PassengerCostModifier
import com.patson.model._
import com.patson.util.CountryCache
import controllers.AuthenticationObject.AuthenticatedAirline
import play.api.libs.json._
import play.api.mvc._

import javax.inject.Inject

class AirportAssetApplication @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  val DISABLEMENT_MESSAGE = "Airport Assets Functionality was disabled for game balance reasons. Assets Purchases are no longer possible"

  implicit object AirportAssetWrites extends Writes[AirportAsset] {
    def writes(entry : AirportAsset): JsValue = {
      val name = entry.status match {
        case AirportAssetStatus.BLUEPRINT => entry.assetType.label
        case _ => entry.name
      }
      var result = Json.obj(
        "airline" -> entry.airline,
        "airport" -> entry.blueprint.airport,
        "assetType" ->  entry.assetType,
        "assetTypeLabel" ->  entry.assetType.label,
        "level" -> entry.level,
        "name" -> name,
        "descriptions" -> entry.assetType.descriptions,
        "constructionDuration" -> entry.assetType.constructionDuration,
        "status" -> entry.status.toString,
        "cost" -> entry.cost,
        "sellValue" -> entry.sellValue,
        "boosts" -> entry.boosts,
        "id" -> entry.id,
        "baseBoosts" -> entry.baseBoosts,
        "publicProperties" -> computeAssetProperties(entry, entry.publicProperties)
      )

      entry.completionCycle.foreach { completionCycle =>
        result = result + ("completionDuration" -> JsNumber(completionCycle - CycleSource.loadCycle()))
      }

      if (entry.isInstanceOf[PassengerCostModifier] && entry.propertyHistoryLastCycle.isDefined) {
        result = result ++ Json.obj(
          "countryRanking" -> entry.paxByCountryCodeLastCycle.toList.sortBy(_._2)(Ordering[Long].reverse).map {
            case (countryCode, paxCount) => Json.obj("countryCode" -> countryCode, "countryName" -> CountryCache.getCountry(countryCode).get.name, "passengerCount" -> paxCount)
          },
          "transitPax" -> entry.transitPaxLastCycle,
          "destinationPax" -> entry.destinationPaxLastCycle)
      }
      result

    }
  }

  object OwnedAirportAssetWrites extends Writes[AirportAsset] {
    def writes(entry : AirportAsset) : JsValue = {
      var result = AirportAssetWrites.writes(entry).asInstanceOf[JsObject]
      val performanceApprox = Math.ceil(entry.performance.toDouble / 100 * 5).toInt
      result = result +
        ("expense" -> JsNumber(entry.expense)) +
        ("revenue" -> JsNumber(entry.revenue)) +
        ("performanceApprox" -> JsNumber(performanceApprox)) + //don't show the actual value. more fun if we hide some details :)
        ("privateProperties" -> Json.toJson(computeAssetProperties(entry, entry.privateProperties)))



      result
    }
  }

  implicit object AirportBoostHistoryWrites extends Writes[AirportAssetBoostHistory] {
    def writes(entry : AirportAssetBoostHistory): JsValue = {
      Json.obj(
        "level" -> entry.level,
        "boostType" -> entry.boostType.toString,
        "label" -> AirportBoostType.getLabel(entry.boostType),
        "value" ->  entry.value,
        "gain" -> entry.gain,
        "upgradeFactor" -> entry.upgradeFactor
      )
    }
  }




  def computeAssetProperties(asset : AirportAsset, rawProperties : Map[String, Long]) : Map[String, String] = {
    val result = collection.mutable.Map[String, String]()
    val formatter = java.text.NumberFormat.getIntegerInstance
    asset match {
      case hotel : HotelAsset =>
        rawProperties.get("occupancy").foreach { occupancy =>
          val occupancyString = formatter.format(occupancy) + " (" + (occupancy * 100 / hotel.capacity) + "%)"
          result.put("Occupancy", occupancyString)
        }
        rawProperties.get("rate").foreach { rate =>
          result.put("Room Rate", "$" + formatter.format(rate))
        }
      case asset : AdmissionAsset =>
        rawProperties.get("visitors").foreach { visitors =>
          val valueString = formatter.format(visitors)
          result.put("Visitors", valueString)
        }
        rawProperties.get("rate").foreach { rate =>
          result.put("Ticket Price", "$" + formatter.format(rate))
        }
      case asset : RentalAsset =>
        rawProperties.get("leasedSpace").foreach { leasedSpace =>
          val valueString = formatter.format(leasedSpace) + " (" + (leasedSpace * 100 / asset.space) + "%)"
          result.put("Leased Floor Space", valueString)
        }
        rawProperties.get("rate100Point").foreach { rate100Point =>
          result.put("Monthly Rent/sq. ft ", "$" + rate100Point.toDouble / 100)
        }
      case _ =>

    }
    result.toMap
  }

  def getAirportAssets(airportId : Int) = Action { request =>
    // Return empty list instead of loading from backend
    Ok(Json.arr())
  }

  def getAirportAssetsWithAirline(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    // Return empty list instead of loading from backend
    Ok(Json.arr())
  }

  def getAirportAssetDetailsWithoutAirline(assetId : Int) = Action { request =>
    // Return empty object instead of loading from backend
    Ok(Json.obj())
  }

  def getAirportAssetDetailsWithAirline(airlineId : Int, assetId : Int) = AuthenticatedAirline(airlineId) { request =>
    // Return empty object instead of loading from backend
    Ok(Json.obj())
  }

  /**
   * Get rejection of building/upgrading the asset
   * @param airline
   * @param asset
   * @return
   */
  def getUpgradeRejection(airline : Airline, asset : AirportAsset) : Option[String] = {
    asset.airline match {
      case Some(owner) =>
        if (owner.id != airline.id) {
          Some(s"Your airline does not own ${asset.name}")
        } else if (airline.getBalance() < asset.cost) {
          Some(s"Not enough cash to upgrade ${asset.name}")
        } else if (asset.level >= AirportAsset.MAX_LEVEL) {
          Some(s"${asset.name} is already at max level")
        } else {
          val cooldownDelta = asset.completionCycle.get + asset.assetType.upgradeCooldown - CycleSource.loadCycle()
          if (cooldownDelta > 0) {
            Some(s"${asset.name} can only be upgraded again in $cooldownDelta week(s)")
          } else {
            None
          }
        }
      case None =>
        airline.getBases().find(_.airport.id == asset.blueprint.airport.id) match {
          case Some(base) =>
            if (airline.getBalance() >= asset.cost) {
              if (base.scale < asset.blueprint.assetType.baseRequirement) {
                Some(s"Requires Airport Base level ${asset.blueprint.assetType.baseRequirement} to build the ${asset.blueprint.assetType.label}")
              } else {
                //only 1 asset per base
                AirportAssetSource.loadAirportAssetsByAirline(airline.id).find(_.blueprint.airport.id == asset.blueprint.airport.id) match {
                  case Some(otherAsset) => Some(s"Cannot build more than 1 asset per airport. Already own ${otherAsset.name}")
                  case None => None //OK
                }
              }
            } else {
              Some(s"Not enough cash to build the ${asset.blueprint.assetType.label}")
            }
          case None => Some(s"Requires Airport Base to build the ${asset.blueprint.assetType.label}")
        }
    }
  }


  def getDowngradeRejection(airline : Airline, asset : AirportAsset) : Option[String] = {
    val owner = asset.airline.get
    if (owner.id != airline.id) {
      Some(s"Your airline does not own ${asset.name}")
    } else if (asset.status != AirportAssetStatus.COMPLETED) {
      Some(s"Cannot downgrade while asset is under construction")
    } else if (asset.level <= 1) {
      Some(s"Cannot downgrade any further")
    } else {
      None
    }
  }

  def getNameRejection(name : String) : Option[String] = {
    if (name.length() < 1 || name.length() > MAX_NAME_LENGTH) {
      Some("Name should be between 1 - " + MAX_NAME_LENGTH + " characters")
    } else if (!name.forall(char => char.isLetter || char == ' ')) {
      Some("Name can only contain space and characters")
    } else {
      None
    }
  }

  val MAX_NAME_LENGTH = 30


  def deleteAirportAsset(airlineId : Int, assetId : Int)= AuthenticatedAirline(airlineId) { request =>
    // Return disablement message instead of backend operations
    BadRequest(Json.obj("error" -> DISABLEMENT_MESSAGE))
  }

  def downgradeAirportAsset(airlineId : Int, assetId : Int) = AuthenticatedAirline(airlineId) { request =>
    // Return disablement message instead of backend operations
    BadRequest(Json.obj("error" -> DISABLEMENT_MESSAGE))
  }

  def putAirportAsset(airlineId : Int, assetId : Int)= AuthenticatedAirline(airlineId) { request =>
    // Return disablement message instead of backend operations
    BadRequest(Json.obj("error" -> DISABLEMENT_MESSAGE))
  }


}