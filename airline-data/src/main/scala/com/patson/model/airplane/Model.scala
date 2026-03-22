package com.patson.model.airplane

import com.patson.model.IdObject
import com.patson.model.Airline
import com.patson.model.airplane.Model.Category
import com.patson.util.AirplaneModelCache

case class Model(
    name: String,
    family: String = "",
    capacity: Int,
    fuelBurn: Int,         // cruise fuel burn, fuel-units/min (integer per original design)
    fuelBurnClimb: Int,    // climb fuel burn, fuel-units/min (absolute, declared per-model)
    speed: Int,
    range: Int,
    price: Int,
    lifespan: Int,
    constructionTime: Int,
    manufacturer: Manufacturer,
    runwayRequirement: Int,
    airplaneType: Model.Type.Type, // explicitly declared in DB/CSV, not inferred at runtime
    introYear: Int,                // calendar year of introduction (e.g. 1958)
    introWeek: Int,                // week of year, 1–52
    climbRateMMin: Int,            // sea-level climb rate, metres/minute
    cruiseAltitudeM: Int,          // target cruise altitude, metres
    imageUrl: String = "",
    var id: Int = 0
) extends IdObject {

  import Model.Type._

  val countryCode: String = manufacturer.countryCode

  val category: Category.Value = Category.fromType(airplaneType)

  val airplaneTypeLabel: String = Model.Type.label(airplaneType)

  // ── Turnaround time (gate + servicing), minutes ──────────────────────────
  private[this] val BASE_TURNAROUND_TIME: Map[Type, Int] = Map(
    SHORT_RANGE_PROP -> 35,
    LONG_RANGE_PROP  -> 75,
    SMALL_PROP       -> 20,
    REGIONAL_PROP    -> 25,
    EARLY_JET        -> 25,
    LIGHT            -> 20,
    SMALL            -> 25,
    REGIONAL         -> 25,
    MEDIUM           -> 30,
    LARGE            -> 40,
    X_LARGE          -> 50,
    JUMBO            -> 60,
    SUPERSONIC       -> 50
  )

  val turnaroundTime: Int =
    BASE_TURNAROUND_TIME(airplaneType) +
      (airplaneType match {
        case SHORT_RANGE_PROP => (capacity / 3).toInt
        case LONG_RANGE_PROP  => (capacity / 3).toInt
        case SMALL_PROP       => (capacity / 5).toInt
        case REGIONAL_PROP    => (capacity / 7).toInt
        case EARLY_JET        => (capacity / 7).toInt
        case LIGHT            => (capacity / 5).toInt
        case SMALL            => (capacity / 5).toInt
        case REGIONAL         => (capacity / 7).toInt
        case MEDIUM           => (capacity / 7).toInt
        case LARGE            => (capacity / 3.5).toInt
        case X_LARGE          => (capacity / 3.5).toInt
        case JUMBO            => (capacity / 3).toInt
        case SUPERSONIC       => (capacity / 2.5).toInt
      })

  // ── Weekly fixed maintenance cost per aircraft, USD ──────────────────────
  val baseMaintenanceCost: Int = {
    val perSeatRate: Int = airplaneType match {
      case SHORT_RANGE_PROP => 85
      case LONG_RANGE_PROP  => 105
      case SMALL_PROP       => 100
      case REGIONAL_PROP    => 110
      case EARLY_JET        => 160
      case LIGHT            => 100
      case SMALL            => 120
      case REGIONAL         => 140
      case MEDIUM           => 150
      case LARGE            => 180
      case X_LARGE          => 200
      case JUMBO            => 220
      case SUPERSONIC       => 300
    }
    perSeatRate * capacity
  }

  // ── Availability ─────────────────────────────────────────────────────────
  /** Game cycle on which this model unlocks. */
  def availabilityCycle: Int = {
    import com.patson.ChronologyConverter._
    ((introYear - WORLD_START_YEAR) * WEEKS_PER_YEAR + (introWeek - 1)) * cyclesPerChronologicalWeek
  }

  // ── Discounts ────────────────────────────────────────────────────────────
  def applyDiscount(discounts: List[ModelDiscount]): Model = {
    var discountedModel = this
    discounts.groupBy(_.discountType).foreach {
      case (discountType, ds) => discountType match {
        case DiscountType.PRICE =>
          val total = ds.map(_.discount).sum
          discountedModel = discountedModel.copy(price = (price * (1 - total)).toInt)
        case DiscountType.CONSTRUCTION_TIME =>
          val total = math.min(1.0, ds.map(_.discount).sum)
          discountedModel = discountedModel.copy(constructionTime = (constructionTime * (1 - total)).toInt)
      }
    }
    discountedModel
  }

  val purchasableWithRelationship: Int => Boolean = relationship =>
    relationship >= Model.BUY_RELATIONSHIP_THRESHOLD
}

// ─────────────────────────────────────────────────────────────────────────────
object Model {
  val BUY_RELATIONSHIP_THRESHOLD = 0

  /** Placeholder instance carrying only an id, for FK-only contexts. */
  def fromId(id: Int): Model = {
    val m = Model(
      name             = "Unknown",
      family           = "",
      capacity         = 0,
      fuelBurn         = 0,
      fuelBurnClimb    = 0,
      speed            = 0,
      range            = 0,
      price            = 0,
      lifespan         = 0,
      constructionTime = 0,
      manufacturer     = Manufacturer("Unknown", countryCode = ""),
      runwayRequirement = 0,
      airplaneType     = Type.MEDIUM,
      introYear        = 1955,
      introWeek        = 1,
      climbRateMMin    = 700,
      cruiseAltitudeM  = 11000
    )
    m.id = id
    m
  }

  // ── Type enumeration ─────────────────────────────────────────────────────
  object Type extends Enumeration {
    type Type = Value
    val SHORT_RANGE_PROP, LONG_RANGE_PROP, SMALL_PROP, REGIONAL_PROP, EARLY_JET,
        LIGHT, SMALL, REGIONAL, MEDIUM, LARGE, X_LARGE, JUMBO, SUPERSONIC = Value

    val label: Type => String = {
      case SHORT_RANGE_PROP => "Short Range Prop"
      case LONG_RANGE_PROP  => "Long Range Prop"
      case SMALL_PROP       => "Small Prop"
      case REGIONAL_PROP    => "Regional Prop"
      case EARLY_JET        => "Early Jet"
      case LIGHT            => "Light"
      case SMALL            => "Small"
      case REGIONAL         => "Regional"
      case MEDIUM           => "Medium"
      case LARGE            => "Large"
      case X_LARGE          => "Extra Large"
      case JUMBO            => "Jumbo"
      case SUPERSONIC       => "Supersonic"
    }
  }

  // ── Category grouping ────────────────────────────────────────────────────
  object Category extends Enumeration {
    type Category = Value
    val LIGHT, REGIONAL, MEDIUM, LARGE, SUPERSONIC = Value

    val grouping: Map[Category, List[Type.Type]] = Map(
      LIGHT      -> List(Type.LIGHT, Type.SMALL, Type.SMALL_PROP),
      REGIONAL   -> List(Type.SHORT_RANGE_PROP, Type.REGIONAL_PROP, Type.REGIONAL),
      MEDIUM     -> List(Type.MEDIUM, Type.EARLY_JET, Type.LONG_RANGE_PROP),
      LARGE      -> List(Type.LARGE, Type.X_LARGE, Type.JUMBO),
      SUPERSONIC -> List(Type.SUPERSONIC)
    )

    val fromType: Type.Value => Category = airplaneType =>
      grouping.find(_._2.contains(airplaneType)).get._1

    val capacityRange: Map[Category, (Int, Int)] = {
      AirplaneModelCache.allModels.map(_._2).groupBy(_.category).view.mapValues { models =>
        val sorted = models.toList.sortBy(_.capacity)
        (sorted.head.capacity, sorted.last.capacity)
      }.toMap
    }

    def getCapacityRange(category: Category): (Int, Int) =
      capacityRange.getOrElse(category, (0, 0))
  }
}