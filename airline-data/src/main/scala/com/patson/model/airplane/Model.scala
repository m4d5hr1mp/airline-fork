package com.patson.model.airplane

// Needed to enable fleet unlocks over time!
import com.patson.ChronologyConverter._

import com.patson.model.IdObject
import com.patson.model.Airline
import com.patson.model.airplane.Model.Category
import com.patson.util.AirplaneModelCache

case class Model(
    name: String,
    family: String = "",
    capacity: Int,
    fuelBurn: Int,
    speed: Int,
    range: Int,
    price: Int,
    lifespan: Int,
    constructionTime: Int,
    manufacturer: Manufacturer,
    runwayRequirement: Int,
    imageUrl: String = "",
    var id: Int = 0
) extends IdObject {
  import Model.Type._

  val countryCode = manufacturer.countryCode
  val SUPERSONIC_SPEED_THRESHOLD = 1236
  val PROP_SPEED_THRESHOLD = 700

  val EARLY_JET_MODELS: Set[String] = Set(
    "Boeing 707-120",
    "DeHaviland Comet 4",
    "Sud-Aviation Caravelle I",
    "Boeing 707-320",
    "Douglas DC-8-10",
    "Douglas DC-8-21",
    "Douglas DC-8-33",
    "DeHaviland Comet 4B",
    "Convair CV-880",
    "Convair CV-880M",
    "Sud-Aviation Caravelle III",
    "Boeing 720",    
    "DeHaviland Comet 4C",
    "Boeing 707-120B",
    "Boeing 720B",
    "Sud-Aviation Caravelle VI",
    "Convair CV-990A",
    "Boeing 707-320B",
    "Tupolev Tu-124",
    "Douglas DC-8-53",
    "Vickers VC-10 Type 1101",
    "Sud-Aviation Caravelle 10B",
  )

  val airplaneType: Type = {
    if (speed > SUPERSONIC_SPEED_THRESHOLD) {
      SUPERSONIC
    } else if (EARLY_JET_MODELS.contains(name)) {
      EARLY_JET
    } else if (speed < PROP_SPEED_THRESHOLD) {
      // === PROPS ===
      if (introYear < 1960) {
        // Post-war props → split by size (larger = longer-range family)
        capacity match {
          case x if x < 50  => SHORT_RANGE_PROP
          case _            => LONG_RANGE_PROP
        }
      } else {
        // Modern props → Small Prop vs Regional Prop
        capacity match {
          case x if x < 50  => SMALL_PROP
          case _            => REGIONAL_PROP
        }
      }
    } else {
      // Jets unchanged
      capacity match {
        case x if x <= 19  => LIGHT
        case x if x <= 50  => SMALL
        case x if x <= 124 => REGIONAL
        case x if x <= 249 => MEDIUM
        case x if x <= 345 => LARGE
        case x if x <= 475 => X_LARGE
        case _             => JUMBO
      }
    }
  }

  val category = Category.fromType(airplaneType)

  private[this] val BASE_TURNAROUND_TIME = Map(
    SHORT_RANGE_PROP -> 15,
    LONG_RANGE_PROP  -> 20,
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

  val turnaroundTime: Int = (
    BASE_TURNAROUND_TIME(airplaneType) +
      (airplaneType match {
        case SHORT_RANGE_PROP => (capacity / 5).toInt
        case LONG_RANGE_PROP  => (capacity / 7).toInt 
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
  )

  val introYear: Int = {
      val weeks = ModelAvailability.modelAvailabilityWeeks.getOrElse(name, 0)
      WORLD_START_YEAR + (weeks / WEEKS_PER_YEAR)
    }

  val airplaneTypeLabel: String = label(airplaneType)

  // Weekly fixed cost per aircraft, in USD (computed as per-seat rate multiplied by maximum certified capacity)
  val baseMaintenanceCost: Int = {
    val perSeatRate: Int = airplaneType match {
      case SHORT_RANGE_PROP => 85
      case LONG_RANGE_PROP  => 105   // long-range props had more complex systems
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

  def applyDiscount(discounts : List[ModelDiscount]) = {
    var discountedModel = this
    discounts.groupBy(_.discountType).foreach {
      case (discountType, discounts) => discountType match {
        case DiscountType.PRICE =>
          val totalDiscount = discounts.map(_.discount).sum
          discountedModel = discountedModel.copy(price = (price * (1 - totalDiscount)).toInt)
        case DiscountType.CONSTRUCTION_TIME =>
          var totalDiscount = discounts.map(_.discount).sum
          totalDiscount = Math.min(1, totalDiscount)
          discountedModel = discountedModel.copy(constructionTime = (constructionTime * (1 - totalDiscount)).toInt)
      }
    }
    discountedModel
  }
  
  val purchasableWithRelationship = (relationship : Int) => {
    relationship >= Model.BUY_RELATIONSHIP_THRESHOLD
}
}

object Model {
  val BUY_RELATIONSHIP_THRESHOLD = 0

  def fromId(id : Int) = {
    val modelWithJustId = Model("Unknown", "Unknown", 0, 0, 0, 0, 0, 0, 0, Manufacturer("Unknown", countryCode = ""), runwayRequirement = 0)
    modelWithJustId.id = id
    modelWithJustId
  }

  object Type extends Enumeration {
    type Type = Value
    val SHORT_RANGE_PROP, LONG_RANGE_PROP, SMALL_PROP, REGIONAL_PROP, EARLY_JET,
        LIGHT, SMALL, REGIONAL, MEDIUM, LARGE, X_LARGE, JUMBO, SUPERSONIC = Value

    val label = (airplaneType: Type) => airplaneType match {
      case SHORT_RANGE_PROP  => "Short Range Prop"
      case LONG_RANGE_PROP   => "Long Range Prop"
      case SMALL_PROP        => "Small Prop"
      case REGIONAL_PROP     => "Regional Prop"
      case EARLY_JET         => "Early Jet"
      case LIGHT             => "Light"
      case SMALL             => "Small"
      case REGIONAL          => "Regional"
      case MEDIUM            => "Medium"
      case LARGE             => "Large"
      case X_LARGE           => "Extra large"
      case JUMBO             => "Jumbo"
      case SUPERSONIC        => "Supersonic"
    }
  }

  object Category extends Enumeration {
    type Category = Value
    val LIGHT, REGIONAL, MEDIUM, LARGE, SUPERSONIC = Value

    val grouping = Map(
        LIGHT      -> List(Type.LIGHT, Type.SMALL, Type.SMALL_PROP),
        REGIONAL   -> List(Type.SHORT_RANGE_PROP, Type.REGIONAL_PROP, Type.REGIONAL),
        MEDIUM     -> List(Type.MEDIUM, Type.EARLY_JET, Type.LONG_RANGE_PROP),
        LARGE      -> List(Type.LARGE, Type.X_LARGE, Type.JUMBO),
        SUPERSONIC -> List(Type.SUPERSONIC)
      )

    val fromType = (airplaneType : Type.Value) => {
      grouping.find(_._2.contains(airplaneType)).get._1
    }

    val capacityRange : Map[Category.Value, (Int, Int)]= {
      AirplaneModelCache.allModels.map(_._2).groupBy(_.category).view.mapValues { models =>
        val sortedByCapacity = models.toList.sortBy(_.capacity)
        (sortedByCapacity.head.capacity, sortedByCapacity.last.capacity)
      }.toMap
    }

    def getCapacityRange(category: Category.Value) = {
      capacityRange.get(category).getOrElse((0, 0))
    }
  }

  //https://en.wikipedia.org/wiki/List_of_jet_airliners
  val models = List(
    Model("Vickers Viscount 700", "Vickers Viscount", capacity = 53, fuelBurn = (1020 / 60).toInt, speed = 521, range = 2140, price = 11_660_000, lifespan = 30 * 52, constructionTime = 4, Manufacturer("Vickers", countryCode = "GB"), runwayRequirement = 1360, imageUrl = ""),
    Model("Lisunov Li-2", "Lisunov Li-2", capacity = 32, fuelBurn = (780 / 60).toInt, speed = 335, range = 2400, price = 6_400_000, lifespan = 25 * 52, constructionTime = 0, Manufacturer("Lisunov", countryCode = "RU"), runwayRequirement = 768, imageUrl = ""),
    Model("Douglas DC-3", "Douglas DC-3", capacity = 32, fuelBurn = (780 / 60).toInt, speed = 335, range = 2400, price = 6_400_000, lifespan = 25 * 52, constructionTime = 0, Manufacturer("Douglas Corporation", countryCode = "US"), runwayRequirement = 768, imageUrl = ""),
    Model("Lockheed L-749 Constellation", "Lockheed Constellation", capacity = 60, fuelBurn = (1200 / 60).toInt, speed = 497, range = 4832, price = 20_700_000, lifespan = 30 * 52, constructionTime = 4, Manufacturer("Lockheed", countryCode = "US"), runwayRequirement = 1113, imageUrl = ""),
    Model("Lockheed L-749A Constellation", "Lockheed Constellation", capacity = 87, fuelBurn = (1860 / 60).toInt, speed = 497, range = 5917, price = 32_625_000, lifespan = 30 * 52, constructionTime = 4, Manufacturer("Lockheed", countryCode = "US"), runwayRequirement = 1244, imageUrl = ""),
    Model("Lockheed L-1049 Super Constellation", "Lockheed Constellation", capacity = 80, fuelBurn = (1980 / 60).toInt, speed = 515, range = 6146, price = 27_600_000, lifespan = 30 * 52, constructionTime = 4, Manufacturer("Lockheed", countryCode = "US"), runwayRequirement = 1199, imageUrl = ""),
    Model("Lockheed L-1049C Super Constellation", "Lockheed Constellation", capacity = 86, fuelBurn = (1860 / 60).toInt, speed = 525, range = 8143, price = 33_970_000, lifespan = 35 * 52, constructionTime = 4, Manufacturer("Lockheed", countryCode = "US"), runwayRequirement = 1435, imageUrl = ""),
    Model("Lockheed L-1049G Super Constellation", "Lockheed Constellation", capacity = 110, fuelBurn = (2400 / 60).toInt, speed = 525, range = 8956, price = 42_350_000, lifespan = 35 * 52, constructionTime = 8, Manufacturer("Lockheed", countryCode = "US"), runwayRequirement = 1525, imageUrl = ""),
    Model("Lockheed L-1649 Starliner", "Lockheed Constellation", capacity = 134, fuelBurn = (3120 / 60).toInt, speed = 563, range = 9275, price = 56_950_000, lifespan = 35 * 52, constructionTime = 8, Manufacturer("Lockheed", countryCode = "US"), runwayRequirement = 1768, imageUrl = ""),
    Model("Douglas DC-6", "Douglas DC-6/DC-7", capacity = 60, fuelBurn = (1200 / 60).toInt, speed = 501, range = 5605, price = 21_000_000, lifespan = 30 * 52, constructionTime = 4, Manufacturer("Douglas Corporation", countryCode = "US"), runwayRequirement = 1406, imageUrl = ""),
    Model("Douglas DC-6B", "Douglas DC-6/DC-7", capacity = 70, fuelBurn = (1440 / 60).toInt, speed = 501, range = 5921, price = 25_550_000, lifespan = 35 * 52, constructionTime = 4, Manufacturer("Douglas Corporation", countryCode = "US"), runwayRequirement = 1590, imageUrl = ""),
    Model("Douglas DC-7", "Douglas DC-6/DC-7", capacity = 74, fuelBurn = (1680 / 60).toInt, speed = 587, range = 8430, price = 31_450_000, lifespan = 35 * 52, constructionTime = 4, Manufacturer("Douglas Corporation", countryCode = "US"), runwayRequirement = 1739, imageUrl = ""),
    Model("Douglas DC-7C", "Douglas DC-6/DC-7", capacity = 112, fuelBurn = (2640 / 60).toInt, speed = 591, range = 9681, price = 45_920_000, lifespan = 35 * 52, constructionTime = 8, Manufacturer("Douglas Corporation", countryCode = "US"), runwayRequirement = 1947, imageUrl = ""),
    Model("Boeing 377 Stratocruiser", "Boeing 377", capacity = 75, fuelBurn = (1680 / 60).toInt, speed = 565, range = 6049, price = 24_375_000, lifespan = 25 * 52, constructionTime = 4, Manufacturer("Boeing", countryCode = "US"), runwayRequirement = 1676, imageUrl = ""),
    Model("Convair CV-240", "Convair CV-240/340/400", capacity = 40, fuelBurn = (780 / 60).toInt, speed = 432, range = 1330, price = 8_400_000, lifespan = 30 * 52, constructionTime = 0, Manufacturer("Convair", countryCode = "US"), runwayRequirement = 790, imageUrl = ""),
    Model("Convair CV-340", "Convair CV-240/340/400", capacity = 52, fuelBurn = (1020 / 60).toInt, speed = 460, range = 2330, price = 13_720_000, lifespan = 30 * 52, constructionTime = 4, Manufacturer("Convair", countryCode = "US"), runwayRequirement = 798, imageUrl = ""),
    Model("Convair CV-440", "Convair CV-240/340/400", capacity = 60, fuelBurn = (1140 / 60).toInt, speed = 460, range = 1521, price = 14_400_000, lifespan = 30 * 52, constructionTime = 4, Manufacturer("Convair", countryCode = "US"), runwayRequirement = 904, imageUrl = ""),
    Model("Vickers Viscount 800", "Vickers Viscount", capacity = 65, fuelBurn = (1500 / 60).toInt, speed = 565, range = 2780, price = 15_600_000, lifespan = 30 * 52, constructionTime = 4, Manufacturer("Vickers-Armstrong", countryCode = "GB"), runwayRequirement = 1650, imageUrl = ""),
    Model("Vickers Viscount 810", "Vickers Viscount", capacity = 75, fuelBurn = (1800 / 60).toInt, speed = 565, range = 2780, price = 18_750_000, lifespan = 30 * 52, constructionTime = 4, Manufacturer("Vickers-Armstrong", countryCode = "GB"), runwayRequirement = 1700, imageUrl = ""),
    Model("Vickers Viking", "Vickers Viking", capacity = 36, fuelBurn = (960 / 60).toInt, speed = 338, range = 2740, price = 7_920_000, lifespan = 35 * 52, constructionTime = 0, Manufacturer("Vickers-Armstrong", countryCode = "GB"), runwayRequirement = 1234, imageUrl = ""),
    Model("Ilyushin Il-18", "Ilyushin Il-18", capacity = 120, fuelBurn = (3300 / 60).toInt, speed = 625, range = 4300, price = 39_000_000, lifespan = 30 * 52, constructionTime = 8, Manufacturer("Ilyushin", countryCode = "RU"), runwayRequirement = 1300, imageUrl = ""),
    Model("Antonov An-10", "Antonov Props", capacity = 84, fuelBurn = (3000 / 60).toInt, speed = 680, range = 1200, price = 24_360_000, lifespan = 30 * 52, constructionTime = 4, Manufacturer("Antonov", countryCode = "UA"), runwayRequirement = 1300, imageUrl = ""),
    Model("Antonov An-24", "Antonov Props", capacity = 55, fuelBurn = (2080 / 60).toInt, speed = 459, range = 1000, price = 11_000_000, lifespan = 40 * 52, constructionTime = 4, Manufacturer("Antonov", countryCode = "UA"), runwayRequirement = 900, imageUrl = ""),
    Model("Xian Y-7", "Xian Props", capacity = 55, fuelBurn = (2080 / 60).toInt, speed = 459, range = 1000, price = 11_000_000, lifespan = 40 * 52, constructionTime = 4, Manufacturer("Xian", countryCode = "CN"), runwayRequirement = 900, imageUrl = ""),
    Model("Handley Page HP81 Hermes", "Handley Page HP81", capacity = 75, fuelBurn = (1560 / 60).toInt, speed = 441, range = 3610, price = 23_250_000, lifespan = 30 * 52, constructionTime = 4, Manufacturer("Handley Page", countryCode = "GB"), runwayRequirement = 1030, imageUrl = ""),
    Model("Boeing 707-120", "Boeing 707", capacity = 174, fuelBurn = (7440 / 60).toInt, speed = 880, range = 5021, price = 81_780_000, lifespan = 30 * 52, constructionTime = 12, Manufacturer("Boeing", countryCode = "US"), runwayRequirement = 2628, imageUrl = ""),
    Model("DeHaviland Comet 4", "DeHaviland Comet", capacity = 81, fuelBurn = (2940 / 60).toInt, speed = 725, range = 5255, price = 31_185_000, lifespan = 30 * 52, constructionTime = 6, Manufacturer("DeHaviland", countryCode = "GB"), runwayRequirement = 2065, imageUrl = ""),
    Model("Sud-Aviation Caravelle I", "Sud-Aviation Caravelle", capacity = 80, fuelBurn = (2520 / 60).toInt, speed = 725, range = 1842, price = 26_400_000, lifespan = 30 * 52, constructionTime = 6, Manufacturer("Sud-Aviation", countryCode = "FR"), runwayRequirement = 2184, imageUrl = ""),
    Model("Boeing 707-320", "Boeing 707", capacity = 189, fuelBurn = (8280 / 60).toInt, speed = 876, range = 7228, price = 93_165_000, lifespan = 30 * 52, constructionTime = 12, Manufacturer("Boeing", countryCode = "US"), runwayRequirement = 3202, imageUrl = ""),
    Model("Douglas DC-8-10", "Douglas DC-8", capacity = 179, fuelBurn = (7140 / 60).toInt, speed = 830, range = 6386, price = 84_130_000, lifespan = 35 * 52, constructionTime = 12, Manufacturer("Douglas Corporation", countryCode = "US"), runwayRequirement = 2875, imageUrl = ""),
    Model("Douglas DC-8-21", "Douglas DC-8", capacity = 179, fuelBurn = (7200 / 60).toInt, speed = 865, range = 7090, price = 85_125_000, lifespan = 35 * 52, constructionTime = 12, Manufacturer("Douglas Corporation", countryCode = "US"), runwayRequirement = 2417, imageUrl = ""),
    Model("Douglas DC-8-33", "Douglas DC-8", capacity = 179, fuelBurn = (7320 / 60).toInt, speed = 880, range = 9571, price = 91_290_000, lifespan = 35 * 52, constructionTime = 12, Manufacturer("Douglas Corporation", countryCode = "US"), runwayRequirement = 2893, imageUrl = ""),
    Model("DeHaviland Comet 4B", "DeHaviland Comet", capacity = 86, fuelBurn = (3060 / 60).toInt, speed = 785, range = 4503, price = 32_680_000, lifespan = 30 * 52, constructionTime = 6, Manufacturer("DeHaviland", countryCode = "GB"), runwayRequirement = 1849, imageUrl = ""),
    Model("Convair CV-880", "Convair CV-880/990", capacity = 110, fuelBurn = (3840 / 60).toInt, speed = 871, range = 5259, price = 42_900_000, lifespan = 30 * 52, constructionTime = 8, Manufacturer("Convair", countryCode = "US"), runwayRequirement = 2333, imageUrl = ""),
    Model("Convair CV-880M", "Convair CV-880/990", capacity = 124, fuelBurn = (4320 / 60).toInt, speed = 893, range = 5508, price = 49_600_000, lifespan = 30 * 52, constructionTime = 8, Manufacturer("Convair", countryCode = "US"), runwayRequirement = 2293, imageUrl = ""),
    Model("Sud-Aviation Caravelle III", "Sud-Aviation Caravelle", capacity = 90, fuelBurn = (2940 / 60).toInt, speed = 725, range = 2341, price = 31_500_000, lifespan = 30 * 52, constructionTime = 6, Manufacturer("Sud-Aviation", countryCode = "FR"), runwayRequirement = 1832, imageUrl = ""),
    Model("Boeing 720", "Boeing 707", capacity = 149, fuelBurn = (6360 / 60).toInt, speed = 880, range = 5616, price = 59_855_000, lifespan = 30 * 52, constructionTime = 12, Manufacturer("Boeing", countryCode = "US"), runwayRequirement = 2764, imageUrl = ""),
    Model("DeHaviland Comet 4C", "DeHaviland Comet", capacity = 101, fuelBurn = (3600 / 60).toInt, speed = 785, range = 4372, price = 39_390_000, lifespan = 30 * 52, constructionTime = 8, Manufacturer("DeHaviland", countryCode = "GB"), runwayRequirement = 2006, imageUrl = ""),
    Model("Boeing 707-120B", "Boeing 707", capacity = 174, fuelBurn = (7260 / 60).toInt, speed = 880, range = 6331, price = 83_520_000, lifespan = 35 * 52, constructionTime = 12, Manufacturer("Boeing", countryCode = "US"), runwayRequirement = 2452, imageUrl = ""),
    Model("Boeing 720B", "Boeing 707", capacity = 156, fuelBurn = (6540 / 60).toInt, speed = 897, range = 6050, price = 62_400_000, lifespan = 35 * 52, constructionTime = 12, Manufacturer("Boeing", countryCode = "US"), runwayRequirement = 1895, imageUrl = ""),
    Model("Sud-Aviation Caravelle VI", "Sud-Aviation Caravelle", capacity = 99, fuelBurn = (3120 / 60).toInt, speed = 725, range = 2788, price = 35_145_000, lifespan = 35 * 52, constructionTime = 8, Manufacturer("Sud-Aviation", countryCode = "FR"), runwayRequirement = 1947, imageUrl = ""),
    Model("Convair CV-990A", "Convair CV-880/990", capacity = 149, fuelBurn = (6960 / 60).toInt, speed = 917, range = 5204, price = 61_835_000, lifespan = 30 * 52, constructionTime = 12, Manufacturer("Convair", countryCode = "US"), runwayRequirement = 2636, imageUrl = ""),
    Model("Boeing 707-320B", "Boeing 707", capacity = 189, fuelBurn = (8048 / 60).toInt, speed = 886, range = 9429, price = 95_445_000, lifespan = 35 * 52, constructionTime = 12, Manufacturer("Boeing", countryCode = "US"), runwayRequirement = 3291, imageUrl = ""),
    Model("Tupolev Tu-124", "Tupolev Tu-124", capacity = 71, fuelBurn = (2580 / 60).toInt, speed = 850, range = 3178, price = 21_300_000, lifespan = 30 * 52, constructionTime = 6, Manufacturer("Tupolev", countryCode = "RU"), runwayRequirement = 1563, imageUrl = ""),
    Model("Douglas DC-8-53", "Douglas DC-8", capacity = 189, fuelBurn = (6960 / 60).toInt, speed = 880, range = 9338, price = 94_500_000, lifespan = 35 * 52, constructionTime = 12, Manufacturer("Douglas Corporation", countryCode = "US"), runwayRequirement = 2827, imageUrl = ""),
    Model("Boeing 727-100", "Boeing 727", capacity = 131, fuelBurn = (4020 / 60).toInt, speed = 960, range = 3881, price = 51_090_000, lifespan = 35 * 52, constructionTime = 8, Manufacturer("Boeing", countryCode = "US"), runwayRequirement = 1508, imageUrl = ""),
    Model("Vickers VC-10 Type 1101", "Vickers VC-10", capacity = 151, fuelBurn = (6720 / 60).toInt, speed = 886, range = 7464, price = 67_950_000, lifespan = 35 * 52, constructionTime = 12, Manufacturer("BAC", countryCode = "GB"), runwayRequirement = 2162, imageUrl = ""),
    Model("Sud-Aviation Caravelle 10B", "Sud-Aviation Caravelle", capacity = 104, fuelBurn = (3120 / 60).toInt, speed = 800, range = 2670, price = 37_440_000, lifespan = 35 * 52, constructionTime = 8, Manufacturer("Sud-Aviation", countryCode = "FR"), runwayRequirement = 1464, imageUrl = ""),
    Model("BAC One-Eleven 200", "BAC One-Eleven", capacity = 89, fuelBurn = (3000 / 60).toInt, speed = 830, range = 2162, price = 29_370_000, lifespan = 35 * 52, constructionTime = 6, Manufacturer("BAC", countryCode = "GB"), runwayRequirement = 1423, imageUrl = ""),
    Model("Douglas DC-9-10", "Douglas DC-9", capacity = 90, fuelBurn = (2760 / 60).toInt, speed = 930, range = 3150, price = 31_500_000, lifespan = 35 * 52, constructionTime = 6, Manufacturer("Douglas Corporation", countryCode = "US"), runwayRequirement = 1411, imageUrl = ""),
    Model("BAC One-Eleven 300", "BAC One-Eleven", capacity = 89, fuelBurn = (2700 / 60).toInt, speed = 830, range = 2565, price = 29_815_000, lifespan = 35 * 52, constructionTime = 6, Manufacturer("BAC", countryCode = "GB"), runwayRequirement = 1981, imageUrl = ""),
    Model("Douglas DC-9-30", "Douglas DC-9", capacity = 130, fuelBurn = (3240/ 60).toInt, speed = 817, range = 2450, price = 47_450_000, lifespan = 35 * 52, constructionTime = 8, Manufacturer("Douglas Corporation", countryCode = "US"), runwayRequirement = 1317, imageUrl = ""),
    Model("Douglas DC-8-61", "Douglas DC-8", capacity = 259, fuelBurn = (8760 / 60).toInt, speed = 845, range = 6847, price = 133_385_000, lifespan = 35 * 52, constructionTime = 18, Manufacturer("Douglas Corporation", countryCode = "US"), runwayRequirement = 2862, imageUrl = ""),
    Model("Douglas DC-8-62", "Douglas DC-8", capacity = 189, fuelBurn = (6420 / 60).toInt, speed = 845, range = 9317, price = 99_225_000, lifespan = 35 * 52, constructionTime = 12, Manufacturer("Douglas Corporation", countryCode = "US"), runwayRequirement = 2808, imageUrl = ""),
    Model("Douglas DC-8-63", "Douglas DC-8", capacity = 259, fuelBurn = (9540 / 60).toInt, speed = 880, range = 8095, price = 141_155_000, lifespan = 35 * 52, constructionTime = 18, Manufacturer("Douglas Corporation", countryCode = "US"), runwayRequirement = 3418, imageUrl = ""),
    Model("Tupolev Tu-134", "Tupolev Tu-134", capacity = 92, fuelBurn = (2640 / 60).toInt, speed = 870, range = 2600, price = 30_820_000, lifespan = 30 * 52, constructionTime = 8, Manufacturer("Tupolev", countryCode = "RU"), runwayRequirement = 1117, imageUrl = ""),
    Model("Ilyushin Il-62", "Ilyushin Il-62", capacity = 186, fuelBurn = (7020 / 60).toInt, speed = 850, range = 7883, price = 79_050_000, lifespan = 30 * 52, constructionTime = 12, Manufacturer("Ilyushin", countryCode = "RU"), runwayRequirement = 2817, imageUrl = ""),
    Model("Boeing 727-200", "Boeing 727", capacity = 189, fuelBurn = (5820 / 60).toInt, speed = 920, range = 3137, price = 82_215_000, lifespan = 35 * 52, constructionTime = 12, Manufacturer("Boeing", countryCode = "US"), runwayRequirement = 2502, imageUrl = ""),
    Model("Douglas DC-9-40", "Douglas DC-9", capacity = 135, fuelBurn = (3240 / 60).toInt, speed = 817, range = 3120, price = 49_950_000, lifespan = 35 * 52, constructionTime = 8, Manufacturer("Douglas Corporation", countryCode = "US"), runwayRequirement = 1457, imageUrl = ""),
    Model("Boeing 737-100", "Boeing 737 Original", capacity = 120, fuelBurn = (3240 / 60).toInt, speed = 890, range = 3185, price = 43_200_000, lifespan = 35 * 52, constructionTime = 8, Manufacturer("Boeing", countryCode = "US"), runwayRequirement = 1981, imageUrl = ""),
    Model("Boeing 737-200", "Boeing 737 Original", capacity = 132, fuelBurn = (3300 / 60).toInt, speed = 890, range = 4306, price = 49_500_000, lifespan = 35 * 52, constructionTime = 8, Manufacturer("Boeing", countryCode = "US"), runwayRequirement = 2097, imageUrl = ""),
    Model("BAC One-Eleven 500", "BAC One-Eleven", capacity = 119, fuelBurn = (3420 / 60).toInt, speed = 742, range = 2745, price = 42_245_000, lifespan = 35 * 52, constructionTime = 8, Manufacturer("BAC", countryCode = "GB"), runwayRequirement = 1981, imageUrl = ""),
    Model("Fokker F28 Fellowship", "Fokker F28", capacity = 80, fuelBurn = (2160 / 60).toInt, speed = 666, range = 2602, price = 26_400_000, lifespan = 35 * 52, constructionTime = 6, Manufacturer("Fokker", countryCode = "NL"), runwayRequirement = 1320, imageUrl = ""),
    Model("Boeing 747-100", "Boeing 747", capacity = 480, fuelBurn = (13450 / 60).toInt, speed = 880, range = 10920, price = 297_600_000, lifespan = 35 * 52, constructionTime = 36, Manufacturer("Boeing", countryCode = "US"), runwayRequirement = 3183, imageUrl = ""),
    
    // Models Below this line have not been rebalanced! 
    Model("Boeing 747-200", "Boeing 747", capacity = 516, fuelBurn = (12800 / 60).toInt, speed = 880, range = 10200, price = 220_700_000, lifespan = 35 * 52, constructionTime = 36, Manufacturer("Boeing", countryCode = "US"), runwayRequirement = 3347, imageUrl = ""),
    Model("Douglas DC-10-10", "Douglas DC-10", capacity = 400, fuelBurn = (7783 / 60).toInt, speed = 925, range = 7323, price = 154_000_000, lifespan = 30 * 52, constructionTime = 30, Manufacturer("Douglas Corporation", countryCode = "US"), runwayRequirement = 3306, imageUrl = ""),
    Model("Tupolev Tu-154", "Tupolev Tu-154", capacity = 168, fuelBurn = (4855 / 60).toInt, speed = 900, range = 5698, price = 55_200_000, lifespan = 30 * 52, constructionTime = 10, Manufacturer("Tupolev", countryCode = "RU"), runwayRequirement = 2104, imageUrl = ""),
    Model("Lockheed L-1011-1", "Lockheed L-1011 Tristar", capacity = 380, fuelBurn = (7452 / 60).toInt, speed = 890, range = 8450, price = 154_000_000, lifespan = 30 * 52, constructionTime = 30, Manufacturer("Lockheed", countryCode = "US"), runwayRequirement = 2245, imageUrl = ""),
    Model("Douglas DC-10-30", "Douglas DC-10", capacity = 380, fuelBurn = (8992 / 60).toInt, speed = 925, range = 10823, price = 165_000_000, lifespan = 30 * 52, constructionTime = 30, Manufacturer("Douglas Corporation", countryCode = "US"), runwayRequirement = 3172, imageUrl = ""),
    Model("Douglas DC-10-40", "Douglas DC-10", capacity = 400, fuelBurn = (9282 / 60).toInt, speed = 925, range = 10254, price = 162_250_000, lifespan = 30 * 52, constructionTime = 30, Manufacturer("Douglas Corporation", countryCode = "US"), runwayRequirement = 2956, imageUrl = ""),
    Model("Tupolev Tu-154A", "Tupolev Tu-154", capacity = 168, fuelBurn = (5823 / 60).toInt, speed = 900, range = 6254, price = 52_500_000, lifespan = 30 * 52, constructionTime = 10, Manufacturer("Tupolev", countryCode = "RU"), runwayRequirement = 2149, imageUrl = ""),
    Model("Ilyushin Il-62M", "Ilyushin Il-62", capacity = 198, fuelBurn = (6493 / 60).toInt, speed = 850, range = 9770, price = 64_650_000, lifespan = 30 * 52, constructionTime = 12, Manufacturer("Ilyushin", countryCode = "RU"), runwayRequirement = 2756, imageUrl = ""),
    Model("Airbus A300B2-100", "Airbus A300/A310", capacity = 336, fuelBurn = (7158 / 60).toInt, speed = 847, range = 3100, price = 119_600_000, lifespan = 30 * 52, constructionTime = 28, Manufacturer("Airbus", countryCode = "NL"), runwayRequirement = 1727, imageUrl = ""),
    Model("Lockheed L-1011-100", "Lockheed L-1011 Tristar", capacity = 380, fuelBurn = (7593 / 60).toInt, speed = 890, range = 9302, price = 159_550_000, lifespan = 30 * 52, constructionTime = 30, Manufacturer("Lockheed", countryCode = "US"), runwayRequirement = 2834, imageUrl = ""),
    Model("Airbus A300B4-200", "Airbus A300/A310", capacity = 336, fuelBurn = (7175 / 60).toInt, speed = 847, range = 5800, price = 134_800_000, lifespan = 30 * 52, constructionTime = 28, Manufacturer("Airbus", countryCode = "NL"), runwayRequirement = 2312, imageUrl = ""),
    Model("Tupolev Tu-154B", "Tupolev Tu-154", capacity = 180, fuelBurn = (7422 / 60).toInt, speed = 900, range = 3612, price = 51_250_000, lifespan = 30 * 52, constructionTime = 12, Manufacturer("Tupolev", countryCode = "RU"), runwayRequirement = 2294, imageUrl = ""),
    Model("Airbus A300B2-200", "Airbus A300/A310", capacity = 336, fuelBurn = (7700 / 60).toInt, speed = 847, range = 3400, price = 101_900_000, lifespan = 30 * 52, constructionTime = 28, Manufacturer("Airbus", countryCode = "NL"), runwayRequirement = 1806, imageUrl = ""),
    Model("Airbus A300B4-100", "Airbus A300/A310", capacity = 336, fuelBurn = (6935 / 60).toInt, speed = 847, range = 4910, price = 124_100_000, lifespan = 30 * 52, constructionTime = 28, Manufacturer("Airbus", countryCode = "NL"), runwayRequirement = 2625, imageUrl = ""),
    Model("Douglas DC-9-50", "Douglas DC-9", capacity = 140, fuelBurn = (2560 / 60).toInt, speed = 817, range = 2566, price = 55_250_000, lifespan = 35 * 52, constructionTime = 8, Manufacturer("Douglas Corporation", countryCode = "US"), runwayRequirement = 1439, imageUrl = ""),
    Model("Boeing 747SP", "Boeing 747", capacity = 384, fuelBurn = (9798 / 60).toInt, speed = 914, range = 13520, price = 167_400_000, lifespan = 35 * 52, constructionTime = 30, Manufacturer("Boeing", countryCode = "US"), runwayRequirement = 2890, imageUrl = ""),
    Model("Lockheed L-1011-200", "Lockheed L-1011 Tristar", capacity = 380, fuelBurn = (7520 / 60).toInt, speed = 890, range = 9414, price = 130_200_000, lifespan = 30 * 52, constructionTime = 30, Manufacturer("Lockheed", countryCode = "US"), runwayRequirement = 2572, imageUrl = ""),
    Model("Lockheed L-1011-500", "Lockheed L-1011 Tristar", capacity = 330, fuelBurn = (8048 / 60).toInt, speed = 900, range = 10298, price = 154_670_000, lifespan = 35 * 52, constructionTime = 30, Manufacturer("Lockheed", countryCode = "US"), runwayRequirement = 2516, imageUrl = ""),
    Model("McDonell-Douglas MD-81", "McDonell-Douglas MD-80", capacity = 172, fuelBurn = (3246 / 60).toInt, speed = 821, range = 3087, price = 74_300_000, lifespan = 35 * 52, constructionTime = 12, Manufacturer("McDonnel-Douglas", countryCode = "US"), runwayRequirement = 2031, imageUrl = ""),
    Model("Ilyushin Il-86", "Ilyushin Il-86", capacity = 350, fuelBurn = (12581 / 60).toInt, speed = 871, range = 4005, price = 120_000_000, lifespan = 30 * 52, constructionTime = 28, Manufacturer("Ilyushin", countryCode = "RU"), runwayRequirement = 3278, imageUrl = ""),
    Model("Yakovlev Yak-42", "Yakovlev Yak-42", capacity = 114, fuelBurn = (3040 / 60).toInt, speed = 820, range = 2876, price = 35_000_000, lifespan = 30 * 52, constructionTime = 8, Manufacturer("Yakovlev", countryCode = "RU"), runwayRequirement = 952, imageUrl = ""),
    Model("Boeing 767-200", "Boeing 767", capacity = 250, fuelBurn = (4425 / 60).toInt, speed = 858, range = 6230, price = 100_000_000, lifespan = 35 * 52, constructionTime = 18, Manufacturer("Boeing", countryCode = "US"), runwayRequirement = 1829, imageUrl = ""),
    Model("Douglas DC-8-71", "Douglas DC-8", capacity = 259, fuelBurn = (5809 / 60).toInt, speed = 850, range = 6840, price = 89_000_000, lifespan = 30 * 52, constructionTime = 18, Manufacturer("McDonnel-Douglas", countryCode = "US"), runwayRequirement = 2716, imageUrl = ""),
    Model("Douglas DC-8-72", "Douglas DC-8", capacity = 189, fuelBurn = (5665 / 60).toInt, speed = 850, range = 9620, price = 85_000_000, lifespan = 30 * 52, constructionTime = 12, Manufacturer("McDonnel-Douglas", countryCode = "US"), runwayRequirement = 2347, imageUrl = ""),
    Model("Boeing 757-200", "Boeing 757", capacity = 239, fuelBurn = (3781 / 60).toInt, speed = 838, range = 7425, price = 98_000_000, lifespan = 35 * 52, constructionTime = 18, Manufacturer("Boeing", countryCode = "US"), runwayRequirement = 2620, imageUrl = ""),
    Model("Boeing 757-200 IGW", "Boeing 757", capacity = 239, fuelBurn = (3980 / 60).toInt, speed = 838, range = 9240, price = 102_000_000, lifespan = 35 * 52, constructionTime = 18, Manufacturer("Boeing", countryCode = "US"), runwayRequirement = 2890, imageUrl = ""),
    Model("BAe-146-200", "BAe-146", capacity = 100, fuelBurn = (2210 / 60).toInt, speed = 747, range = 3340, price = 48_500_000, lifespan = 30 * 52, constructionTime = 8, Manufacturer("British Aerospace", countryCode = "GB"), runwayRequirement = 1390, imageUrl = ""),
    Model("Douglas DC-8-73", "Douglas DC-8", capacity = 259, fuelBurn = (6278 / 60).toInt, speed = 850, range = 8950, price = 92_500_000, lifespan = 30 * 52, constructionTime = 18, Manufacturer("McDonnel-Douglas", countryCode = "US"), runwayRequirement = 3050, imageUrl = ""),
    Model("Boeing 747-300", "Boeing 747", capacity = 608, fuelBurn = (12270 / 60).toInt, speed = 880, range = 10967, price = 276_800_000, lifespan = 35 * 52, constructionTime = 42, Manufacturer("Boeing", countryCode = "US"), runwayRequirement = 3299, imageUrl = ""),
    Model("Airbus A310-300", "Airbus A300/A310", capacity = 264, fuelBurn = (4840 / 60).toInt, speed = 835, range = 8364, price = 125_000_000, lifespan = 35 * 52, constructionTime = 18, Manufacturer("Airbus", countryCode = "NL"), runwayRequirement = 2285, imageUrl = ""),
    Model("BAe-146-100", "BAe-146", capacity = 82, fuelBurn = (2174 / 60).toInt, speed = 747, range = 3872, price = 44_750_000, lifespan = 30 * 52, constructionTime = 8, Manufacturer("British Aerospace", countryCode = "GB"), runwayRequirement = 1195, imageUrl = ""),
    Model("Boeing 767-200ER", "Boeing 767", capacity = 250, fuelBurn = (4565 / 60).toInt, speed = 858, range = 10042, price = 147_500_000, lifespan = 35 * 52, constructionTime = 18, Manufacturer("Boeing", countryCode = "US"), runwayRequirement = 2290, imageUrl = ""),
    Model("Tupolev Tu-154M", "Tupolev Tu-154", capacity = 180, fuelBurn = (5572 / 60).toInt, speed = 900, range = 5817, price = 57_500_000, lifespan = 35 * 52, constructionTime = 12, Manufacturer("Tupolev", countryCode = "RU"), runwayRequirement = 2478, imageUrl = ""),
    Model("Airbus A300B4-600", "Airbus A300/A310", capacity = 345, fuelBurn = (6634 / 60).toInt, speed = 847, range = 6288, price = 161_600_000, lifespan = 35 * 52, constructionTime = 28, Manufacturer("Airbus", countryCode = "NL"), runwayRequirement = 2194, imageUrl = ""),
    Model("Saab 340", "Saab Regional", capacity = 34, fuelBurn = (550 / 60).toInt, speed = 463, range = 1732, price = 18_600_000, lifespan = 30 * 52, constructionTime = 4, Manufacturer("Saab", countryCode = "SE"), runwayRequirement = 1395, imageUrl = ""),
    Model("Bombardier DHC-8-100", "DHC-8", capacity = 40, fuelBurn = (600 / 60).toInt, speed = 463, range = 1795, price = 24_800_000, lifespan = 30 * 52, constructionTime = 4, Manufacturer("Bombardier", countryCode = "CA"), runwayRequirement = 1067, imageUrl = ""),
    Model("Boeing 737-500", "Boeing 737 Classic", capacity = 132, fuelBurn = (2138 / 60).toInt, speed = 796, range = 5443, price = 61_500_000, lifespan = 35 * 52, constructionTime = 6, Manufacturer("Boeing", countryCode = "US"), runwayRequirement = 1832, imageUrl = ""),
    Model("McDonell-Douglas MD-83", "McDonell-Douglas MD-80", capacity = 172, fuelBurn = (3447 / 60).toInt, speed = 821, range = 4986, price = 75_250_000, lifespan = 35 * 52, constructionTime = 8, Manufacturer("McDonnel-Douglas", countryCode = "US"), runwayRequirement = 2925, imageUrl = ""),
    Model("Embraer EMB120 Brasilia", "Embraer ERJ", capacity = 30, fuelBurn = (412 / 60).toInt, speed = 482, range = 1750, price = 17_900_000, lifespan = 30 * 52, constructionTime = 4, Manufacturer("Embraer", countryCode = "BR"), runwayRequirement = 1420, imageUrl = ""),
    Model("ATR 42-600", "ATR-Regional", capacity = 52, fuelBurn = (811 / 60).toInt, speed = 556, range = 1302, price = 27_300_000, lifespan = 30 * 52, constructionTime = 4, Manufacturer("ATR", countryCode = "FR"), runwayRequirement = 1165, imageUrl = ""),
    Model("ATR 42-300", "ATR-Regional", capacity = 40, fuelBurn = (568 / 60).toInt, speed = 484, range = 850, price = 29_900_000, lifespan = 30 * 52, constructionTime = 4, Manufacturer("ATR", countryCode = "FR"), runwayRequirement = 1267, imageUrl = ""),
    Model("Airbus A310-200", "Airbus A300/A310", capacity = 264, fuelBurn = (5241 / 60).toInt, speed = 835, range = 6906, price = 115_500_000, lifespan = 35 * 52, constructionTime = 18, Manufacturer("Airbus", countryCode = "NL"), runwayRequirement = 2049, imageUrl = ""),
    Model("Boeing 767-300", "Boeing 767", capacity = 290, fuelBurn = (5207 / 60).toInt, speed = 858, range = 7883, price = 137_500_000, lifespan = 35 * 52, constructionTime = 18, Manufacturer("Boeing", countryCode = "US"), runwayRequirement = 2418, imageUrl = ""),
    Model("SAIC MD-81", "SAIC MD-80", capacity = 172, fuelBurn = (3246 / 60).toInt, speed = 821, range = 3087, price = 70_000_000, lifespan = 35 * 52, constructionTime = 12, Manufacturer("SAIC", countryCode = "CN"), runwayRequirement = 2031, imageUrl = ""),
    Model("Fokker 50", "Fokker", capacity = 60, fuelBurn = (912 / 60).toInt, speed = 471, range = 2400, price = 36_800_000, lifespan = 30 * 52, constructionTime = 4, Manufacturer("Fokker", countryCode = "NL"), runwayRequirement = 1350, imageUrl = ""),
    Model("McDonell-Douglas MD-87", "McDonell-Douglas MD-80", capacity = 139, fuelBurn = (3780 / 60).toInt, speed = 821, range = 4393, price = 63_650_000, lifespan = 35 * 52, constructionTime = 8, Manufacturer("McDonnel-Douglas", countryCode = "US"), runwayRequirement = 1859, imageUrl = ""),
    Model("Fokker 100", "Fokker", capacity = 109, fuelBurn = (2000 / 60).toInt, speed = 845, range = 3170, price = 55_750_000, lifespan = 35 * 52, constructionTime = 8, Manufacturer("Fokker", countryCode = "NL"), runwayRequirement = 1621, imageUrl = ""),
    Model("Airbus A320-100", "Airvus A320", capacity = 180, fuelBurn = (2688 / 60).toInt, speed = 829, range = 4014, price = 81_600_000, lifespan = 35 * 52, constructionTime = 12, Manufacturer("Airbus", countryCode = "NL"), runwayRequirement = 2123, imageUrl = ""),
    Model("Boeing 767-300ER", "Boeing 767", capacity = 290, fuelBurn = (5416 / 60).toInt, speed = 858, range = 11484, price = 191_800_000, lifespan = 35 * 52, constructionTime = 20, Manufacturer("Boeing", countryCode = "US"), runwayRequirement = 2712, imageUrl = ""),
    Model("Boeing 737-400", "Boeing 737 Classic", capacity = 188, fuelBurn = (2932 / 60).toInt, speed = 796, range = 5184, price = 83_800_000, lifespan = 35 * 52, constructionTime = 12, Manufacturer("Boeing", countryCode = "US"), runwayRequirement = 2540, imageUrl = ""),
    Model("SAIC MD-83", "SAIC MD-80", capacity = 172, fuelBurn = (3447 / 60).toInt, speed = 821, range = 4986, price = 72_500_000, lifespan = 35 * 52, constructionTime = 12, Manufacturer("SAIC", countryCode = "CN"), runwayRequirement = 2925, imageUrl = ""),
    Model("Bombardier DHC-8-200", "DHC-8", capacity = 40, fuelBurn = (600 / 60).toInt, speed = 463, range = 1795, price = 22_400_000, lifespan = 30 * 52, constructionTime = 4, Manufacturer("Bombardier", countryCode = "CA"), runwayRequirement = 1067, imageUrl = ""),
    Model("Boeing 747-400", "Boeing 747", capacity = 634, fuelBurn = (11287 / 60).toInt, speed = 880, range = 13632, price = 405_000_000, lifespan = 35 * 52, constructionTime = 42, Manufacturer("Boeing", countryCode = "US"), runwayRequirement = 3315, imageUrl = ""),
    Model("BAe-146-300", "BAe-146", capacity = 112, fuelBurn = (2125 / 60).toInt, speed = 747, range = 2970, price = 52_500_000, lifespan = 30 * 52, constructionTime = 6, Manufacturer("British Aerospace", countryCode = "GB"), runwayRequirement = 1535, imageUrl = ""),
    Model("Boeing 737-300", "Boeing 737 Classic", capacity = 149, fuelBurn = (2272 / 60).toInt, speed = 796, range = 5443, price = 68_500_000, lifespan = 35 * 52, constructionTime = 8, Manufacturer("Boeing", countryCode = "US"), runwayRequirement = 1939, imageUrl = ""),
    Model("McDonnell-Douglas MD-11", "McDonnell-Douglas MD-11", capacity = 410, fuelBurn = (7857 / 60).toInt, speed = 875, range = 12778, price = 246_000_000, lifespan = 35 * 52, constructionTime = 30, Manufacturer("McDonnel-Douglas", countryCode = "US"), runwayRequirement = 2972, imageUrl = ""),
    Model("SAIC MD-87", "SAIC MD-80", capacity = 139, fuelBurn = (3780 / 60).toInt, speed = 821, range = 4393, price = 57_500_000, lifespan = 35 * 52, constructionTime = 8, Manufacturer("SAIC", countryCode = "CN"), runwayRequirement = 1859, imageUrl = ""),
    Model("Airbus A340-300", "Airbus A340", capacity = 420, fuelBurn = (6832 / 60).toInt, speed = 871, range = 12259, price = 252_000_000, lifespan = 35 * 52, constructionTime = 30, Manufacturer("Airbus", countryCode = "NL"), runwayRequirement = 2743, imageUrl = ""),
    Model("Airbus A340-200", "Airbus A340", capacity = 404, fuelBurn = (6691 / 60).toInt, speed = 871, range = 14026, price = 241_000_000, lifespan = 35 * 52, constructionTime = 30, Manufacturer("Airbus", countryCode = "NL"), runwayRequirement = 2743, imageUrl = ""),
    Model("Ilyushin Il-96-400", "Ilyushin Il-96", capacity = 430, fuelBurn = (9760 / 60).toInt, speed = 871, range = 11550, price = 195_000_000, lifespan = 30 * 52, constructionTime = 30, Manufacturer("Ilyushin", countryCode = "RU"), runwayRequirement = 2780, imageUrl = ""),
    Model("Avro RJ85", "Avro RJ", capacity = 100, fuelBurn = (1820 / 60).toInt, speed = 720, range = 2095, price = 53_450_000, lifespan = 30 * 52, constructionTime = 8, Manufacturer("British Aerospace", countryCode = "GB"), runwayRequirement = 1390, imageUrl = ""),
    Model("Avro RJ70", "Avro RJ", capacity = 82, fuelBurn = (1551 / 60).toInt, speed = 720, range = 3870, price = 49_750_000, lifespan = 30 * 52, constructionTime = 6, Manufacturer("British Aerospace", countryCode = "GB"), runwayRequirement = 1095, imageUrl = ""),
    Model("Avro RJ100", "Avro RJ", capacity = 112, fuelBurn = (1932 / 60).toInt, speed = 720, range = 3340, price = 55_650_000, lifespan = 30 * 52, constructionTime = 8, Manufacturer("British Aerospace", countryCode = "GB"), runwayRequirement = 1535, imageUrl = ""),
    Model("Airbus A330-300", "Airbus A330", capacity = 420, fuelBurn = (6317 / 60).toInt, speed = 860, range = 10510, price = 252_200_000, lifespan = 35 * 52, constructionTime = 30, Manufacturer("Airbus", countryCode = "NL"), runwayRequirement = 2209, imageUrl = ""),
    Model("Airbus A321-100", "Airbus A320", capacity = 220, fuelBurn = (3211 / 60).toInt, speed = 829, range = 3773, price = 112_525_000, lifespan = 35 * 52, constructionTime = 18, Manufacturer("Airbus", countryCode = "NL"), runwayRequirement = 2187, imageUrl = ""),
    Model("Bombardier CRJ200", "Bombardier CRJ", capacity = 50, fuelBurn = (1125 / 60).toInt, speed = 786, range = 3713, price = 39_700_000, lifespan = 35 * 52, constructionTime = 4, Manufacturer("Bombardier", countryCode = "CA"), runwayRequirement = 1770, imageUrl = ""),
    Model("Saab 2000", "Saab Regional", capacity = 58, fuelBurn = (600 / 60).toInt, speed = 594, range = 2868, price = 28_800_000, lifespan = 30 * 52, constructionTime = 4, Manufacturer("Saab", countryCode = "SE"), runwayRequirement = 1252, imageUrl = ""),
    Model("McDonnell-Douglas MD-11ER", "McDonnell-Douglas MD-11", capacity = 410, fuelBurn = (7857 / 60).toInt, speed = 875, range = 13430, price = 217_000_000, lifespan = 35 * 52, constructionTime = 30, Manufacturer("McDonnel-Douglas", countryCode = "US"), runwayRequirement = 3280, imageUrl = ""),
    Model("Fokker 70ER", "Fokker", capacity = 79, fuelBurn = (1777 / 60).toInt, speed = 845, range = 3410, price = 43_400_000, lifespan = 30 * 52, constructionTime = 6, Manufacturer("Fokker", countryCode = "NL"), runwayRequirement = 1300, imageUrl = ""),
    Model("Fokker 70", "Fokker", capacity = 79, fuelBurn = (1678 / 60).toInt, speed = 845, range = 2010, price = 39_100_000, lifespan = 30 * 52, constructionTime = 6, Manufacturer("Fokker", countryCode = "NL"), runwayRequirement = 1300, imageUrl = ""),
    Model("Bombardier DHC-8-300", "DHC-8", capacity = 56, fuelBurn = (896 / 60).toInt, speed = 528, range = 1500, price = 26_000_000, lifespan = 30 * 52, constructionTime = 4, Manufacturer("Bombardier", countryCode = "CA"), runwayRequirement = 1085, imageUrl = ""),
    Model("McDonnell Douglas MD-90-30", "McDonnell Douglas MD-90", capacity = 153, fuelBurn = (2409 / 60).toInt, speed = 814, range = 3862, price = 84_400_000, lifespan = 35 * 52, constructionTime = 8, Manufacturer("McDonnel-Douglas", countryCode = "US"), runwayRequirement = 2000, imageUrl = ""),
    Model("Boeing 777-200", "Boeing 777", capacity = 420, fuelBurn = (6495 / 60).toInt, speed = 875, range = 9574, price = 257_400_000, lifespan = 35 * 52, constructionTime = 30, Manufacturer("Boeing", countryCode = "US"), runwayRequirement = 2561, imageUrl = ""),
    Model("ATR 42-500", "ATR-Regional", capacity = 48, fuelBurn = (796 / 60).toInt, speed = 535, range = 1345, price = 25_500_000, lifespan = 30 * 52, constructionTime = 4, Manufacturer("ATR", countryCode = "FR"), runwayRequirement = 1278, imageUrl = ""),
    Model("Tupolev Tu-204-100", "Tupolev Tu", capacity = 210, fuelBurn = (2937 / 60).toInt, speed = 850, range = 6810, price = 95_000_000, lifespan = 35 * 52, constructionTime = 12, Manufacturer("Tupolev", countryCode = "RU"), runwayRequirement = 2050, imageUrl = ""),
    Model("Airbus A319-100", "Airbus A320", capacity = 156, fuelBurn = (2475 / 60).toInt, speed = 829, range = 6845, price = 82_250_000, lifespan = 35 * 52, constructionTime = 8, Manufacturer("Airbus", countryCode = "NL"), runwayRequirement = 2103, imageUrl = ""),
    Model("Airbus A320-200", "Airbus A320", capacity = 180, fuelBurn = (2520 / 60).toInt, speed = 829, range = 5816, price = 88_750_000, lifespan = 35 * 52, constructionTime = 12, Manufacturer("Airbus", countryCode = "NL"), runwayRequirement = 2061, imageUrl = ""),
    Model("ATR 72-600", "ATR-Regional", capacity = 78, fuelBurn = (1209 / 60).toInt, speed = 510, range = 1370, price = 34_100_000, lifespan = 30 * 52, constructionTime = 6, Manufacturer("ATR", countryCode = "FR"), runwayRequirement = 1433, imageUrl = ""),
    Model("Boeing 777-200ER", "Boeing 777", capacity = 420, fuelBurn = (7092 / 60).toInt, speed = 875, range = 13891, price = 309_000_000, lifespan = 35 * 52, constructionTime = 30, Manufacturer("Boeing", countryCode = "US"), runwayRequirement = 2890, imageUrl = ""),
    Model("Embraer ERJ145", "Embraer ERJ", capacity = 50, fuelBurn = (1100 / 60).toInt, speed = 850, range = 2870, price = 38_100_000, lifespan = 35 * 52, constructionTime = 4, Manufacturer("Embraer", countryCode = "BR"), runwayRequirement = 1560, imageUrl = ""),
    Model("Airbus A321-200", "Airbus A320", capacity = 236, fuelBurn = (3246 / 60).toInt, speed = 829, range = 5518, price = 132_300_000, lifespan = 35 * 52, constructionTime = 18, Manufacturer("Airbus", countryCode = "NL"), runwayRequirement = 2323, imageUrl = ""),
    Model("McDonnell Douglas MD-90-30ER", "McDonnell Douglas MD-90", capacity = 153, fuelBurn = (2478 / 60).toInt, speed = 814, range = 4426, price = 86_875_000, lifespan = 35 * 52, constructionTime = 8, Manufacturer("McDonnel-Douglas", countryCode = "US"), runwayRequirement = 2134, imageUrl = ""),
    Model("Boeing 737-700", "Boeing 737", capacity = 149, fuelBurn = (1996 / 60).toInt, speed = 829, range = 6230, price = 88_700_000, lifespan = 35 * 52, constructionTime = 8, Manufacturer("Boeing", countryCode = "US"), runwayRequirement = 1800, imageUrl = ""),
    Model("Airbus A330-200", "Airbus A330", capacity = 360, fuelBurn = (7059 / 60).toInt, speed = 860, range = 13092, price = 254_100_000, lifespan = 35 * 52, constructionTime = 28, Manufacturer("Airbus", countryCode = "NL"), runwayRequirement = 2445, imageUrl = ""),
    Model("Boeing 737-800", "Boeing 737", capacity = 184, fuelBurn = (2484 / 60).toInt, speed = 829, range = 5840, price = 105_000_000, lifespan = 35 * 52, constructionTime = 12, Manufacturer("Boeing", countryCode = "US"), runwayRequirement = 1903, imageUrl = ""),
    Model("Boeing 777-300", "Boeing 777", capacity = 528, fuelBurn = (7547 / 60).toInt, speed = 875, range = 11387, price = 338_300_000, lifespan = 35 * 52, constructionTime = 40, Manufacturer("Boeing", countryCode = "US"), runwayRequirement = 3186, imageUrl = ""),
    Model("Boeing 737-600", "Boeing 737", capacity = 132, fuelBurn = (1953 / 60).toInt, speed = 829, range = 5650, price = 78_800_000, lifespan = 35 * 52, constructionTime = 8, Manufacturer("Boeing", countryCode = "US"), runwayRequirement = 2000, imageUrl = ""),
    Model("Boeing 757-300", "Boeing 757", capacity = 279, fuelBurn = (4505 / 60).toInt, speed = 838, range = 6681, price = 148_600_000, lifespan = 35 * 52, constructionTime = 24, Manufacturer("Boeing", countryCode = "US"), runwayRequirement = 2618, imageUrl = ""),
    Model("Embraer ERJ135", "Embraer ERJ", capacity = 37, fuelBurn = (907 / 60).toInt, speed = 829, range = 3240, price = 29_000_000, lifespan = 35 * 52, constructionTime = 4, Manufacturer("Embraer", countryCode = "BR"), runwayRequirement = 1580, imageUrl = ""),
    Model("McDonnell Douglas MD-90-40", "McDonnell Douglas MD-90", capacity = 172, fuelBurn = (2717 / 60).toInt, speed = 814, range = 3537, price = 88_600_000, lifespan = 35 * 52, constructionTime = 12, Manufacturer("McDonnel-Douglas", countryCode = "US"), runwayRequirement = 2300, imageUrl = ""),
    Model("McDonnell Douglas MD-95", "McDonnell Douglas MD-90", capacity = 134, fuelBurn = (1846 / 60).toInt, speed = 816, range = 2645, price = 67_500_000, lifespan = 35 * 52, constructionTime = 8, Manufacturer("McDonnel-Douglas", countryCode = "US"), runwayRequirement = 2100, imageUrl = ""),
    Model("Bombardier DHC-8-400", "Bombardier DHC-8", capacity = 78, fuelBurn = (1014 / 60).toInt, speed = 667, range = 2037, price = 37_400_000, lifespan = 30 * 52, constructionTime = 6, Manufacturer("Bombardier", countryCode = "CA"), runwayRequirement = 1277, imageUrl = ""),
    Model("SAIC MD-90-30", "SAIC MD-90", capacity = 187, fuelBurn = (2973 / 60).toInt, speed = 814, range = 5005, price = 77_500_000, lifespan = 35 * 52, constructionTime = 12, Manufacturer("SAIC", countryCode = "CN"), runwayRequirement = 2550, imageUrl = ""),
    Model("Boeing 767-400ER", "Boeing 767", capacity = 409, fuelBurn = (5934 / 60).toInt, speed = 858, range = 10418, price = 265_000_000, lifespan = 35 * 52, constructionTime = 28, Manufacturer("Boeing", countryCode = "US"), runwayRequirement = 3171, imageUrl = ""),
    Model("Bombardier CRJ700", "Bombardier CRJ", capacity = 78, fuelBurn = (1560 / 60).toInt, speed = 828, range = 2593, price = 47_300_000, lifespan = 35 * 52, constructionTime = 6, Manufacturer("Bombardier", countryCode = "CA"), runwayRequirement = 1516, imageUrl = ""),
    Model("McDonnell Douglas MD-90-55", "McDonnell Douglas MD-90", capacity = 187, fuelBurn = (2973 / 60).toInt, speed = 814, range = 5005, price = 95_000_000, lifespan = 35 * 52, constructionTime = 12, Manufacturer("McDonnel-Douglas", countryCode = "US"), runwayRequirement = 2550, imageUrl = ""),
    Model("SAIC MD-90-30ER", "SAIC MD-90", capacity = 172, fuelBurn = (2717 / 60).toInt, speed = 814, range = 3537, price = 80_000_000, lifespan = 35 * 52, constructionTime = 12, Manufacturer("SAIC", countryCode = "CN"), runwayRequirement = 2300, imageUrl = ""),
    Model("Boeing 737-900", "Boeing 737", capacity = 198, fuelBurn = (2920 / 60).toInt, speed = 829, range = 5556, price = 116_800_000, lifespan = 35 * 52, constructionTime = 12, Manufacturer("Boeing", countryCode = "US"), runwayRequirement = 2179, imageUrl = ""),
    Model("Embraer ERJ140", "Embraer ERJ", capacity = 44, fuelBurn = (1056 / 60).toInt, speed = 828, range = 3000, price = 28_400_000, lifespan = 35 * 52, constructionTime = 4, Manufacturer("Embraer", countryCode = "BR"), runwayRequirement = 1970, imageUrl = ""),
    Model("Airbus A340-600", "Airbus A340", capacity = 475, fuelBurn = (8938 / 60).toInt, speed = 871, range = 13797, price = 336_900_000, lifespan = 35 * 52, constructionTime = 30, Manufacturer("Airbus", countryCode = "NL"), runwayRequirement = 3140, imageUrl = ""),
    Model("Embraer ERJ145XR", "Embraer ERJ", capacity = 50, fuelBurn = (1200 / 60).toInt, speed = 850, range = 3700, price = 38_500_000, lifespan = 35 * 52, constructionTime = 4, Manufacturer("Embraer", countryCode = "BR"), runwayRequirement = 1720, imageUrl = ""),
    Model("SAIC MD-90-40", "SAIC MD-90", capacity = 153, fuelBurn = (2478 / 60).toInt, speed = 814, range = 4426, price = 82_500_000, lifespan = 35 * 52, constructionTime = 8, Manufacturer("SAIC", countryCode = "CN"), runwayRequirement = 2134, imageUrl = ""),
    Model("Boeing 747-400ER", "Boeing 747", capacity = 634, fuelBurn = (11287 / 60).toInt, speed = 880, range = 14460, price = 411_700_000, lifespan = 35 * 52, constructionTime = 40, Manufacturer("Boeing", countryCode = "US"), runwayRequirement = 3400, imageUrl = ""),
    Model("Bombardier CRJ900", "Bombardier CRJ", capacity = 90, fuelBurn = (1912 / 60).toInt, speed = 828, range = 2876, price = 50_800_000, lifespan = 35 * 52, constructionTime = 8, Manufacturer("Bombardier", countryCode = "CA"), runwayRequirement = 1760, imageUrl = ""),
    Model("Airbus A318", "Airbus A320", capacity = 136, fuelBurn = (2532 / 60).toInt, speed = 829, range = 5740, price = 69_500_000, lifespan = 35 * 52, constructionTime = 12, Manufacturer("Airbus", countryCode = "NL"), runwayRequirement = 1780, imageUrl = ""),
    Model("Airbus A340-500", "Airbus A340", capacity = 420, fuelBurn = (8559 / 60).toInt, speed = 871, range = 16942, price = 327_300_000, lifespan = 35 * 52, constructionTime = 30, Manufacturer("Airbus", countryCode = "NL"), runwayRequirement = 3151, imageUrl = ""),
    Model("Embraer EMB170", "Embraer ERJ", capacity = 72, fuelBurn = (1386 / 60).toInt, speed = 829, range = 3334, price = 42_500_000, lifespan = 35 * 52, constructionTime = 6, Manufacturer("Embraer", countryCode = "BR"), runwayRequirement = 1644, imageUrl = ""),
    Model("Boeing 777-300ER", "Boeing 777", capacity = 528, fuelBurn = (8416 / 60).toInt, speed = 875, range = 15017, price = 374_000_000, lifespan = 35 * 52, constructionTime = 40, Manufacturer("Boeing", countryCode = "US"), runwayRequirement = 3046, imageUrl = ""),
    Model("SAIC MD-90-55", "SAIC MD-90", capacity = 153, fuelBurn = (2409 / 60).toInt, speed = 814, range = 3862, price = 85_000_000, lifespan = 35 * 52, constructionTime = 8, Manufacturer("SAIC", countryCode = "CN"), runwayRequirement = 2000, imageUrl = ""),
    Model("Tupolev Tu-204-300", "Tupolev Tu", capacity = 164, fuelBurn = (2705 / 60).toInt, speed = 850, range = 6000, price = 61_200_000, lifespan = 35 * 52, constructionTime = 8, Manufacturer("Tupolev", countryCode = "RU"), runwayRequirement = 2050, imageUrl = ""),
    Model("Embraer EMB175", "Embraer ERJ", capacity = 88, fuelBurn = (1518 / 60).toInt, speed = 829, range = 3521, price = 42_500_000, lifespan = 35 * 52, constructionTime = 4, Manufacturer("Embraer", countryCode = "BR"), runwayRequirement = 1612, imageUrl = ""),
    Model("Embraer EMB190", "Embraer ERJ", capacity = 108, fuelBurn = (1809 / 60).toInt, speed = 829, range = 4445, price = 61_900_000, lifespan = 35 * 52, constructionTime = 6, Manufacturer("Embraer", countryCode = "BR"), runwayRequirement = 1890, imageUrl = ""),
    Model("Boeing 777-200LR", "Boeing 777", capacity = 420, fuelBurn = (7873 / 60).toInt, speed = 875, range = 17305, price = 369_000_000, lifespan = 35 * 52, constructionTime = 30, Manufacturer("Boeing", countryCode = "US"), runwayRequirement = 2805, imageUrl = ""),
    Model("Embraer EMB195", "Embraer ERJ", capacity = 124, fuelBurn = (1891 / 60).toInt, speed = 829, range = 3334, price = 63_900_000, lifespan = 35 * 52, constructionTime = 8, Manufacturer("Embraer", countryCode = "BR"), runwayRequirement = 2080, imageUrl = ""),
    Model("Boeing 737-700ER", "Boeing 737", capacity = 149, fuelBurn = (2070 / 60).toInt, speed = 829, range = 10200, price = 110_000_000, lifespan = 35 * 52, constructionTime = 12, Manufacturer("Boeing", countryCode = "US"), runwayRequirement = 2196, imageUrl = ""),
    Model("Boeing 737-900ER", "Boeing 737", capacity = 215, fuelBurn = (3063 / 60).toInt, speed = 829, range = 5756, price = 125_250_000, lifespan = 35 * 52, constructionTime = 18, Manufacturer("Boeing", countryCode = "US"), runwayRequirement = 2919, imageUrl = ""),
    Model("Comac ARJ21", "Comac ARJ", capacity = 90, fuelBurn = (1912 / 60).toInt, speed = 828, range = 2876, price = 51_000_000, lifespan = 35 * 52, constructionTime = 6, Manufacturer("COMAC", countryCode = "CN"), runwayRequirement = 1760, imageUrl = ""),
    Model("Airbus A380-800", "Airbus A380", capacity = 820, fuelBurn = (13493 / 60).toInt, speed = 875, range = 15199, price = 492_000_000, lifespan = 35 * 52, constructionTime = 48, Manufacturer("Airbus", countryCode = "NL"), runwayRequirement = 3004, imageUrl = ""),
    Model("Ilyushin Il-96-300", "Ilyushin Il-96", capacity = 300, fuelBurn = (9644 / 60).toInt, speed = 871, range = 9661, price = 260_000_000, lifespan = 35 * 52, constructionTime = 28, Manufacturer("Ilyushin", countryCode = "RU"), runwayRequirement = 2936, imageUrl = ""),
    Model("Bombardier DHC-6-400", "DHC-8", capacity = 19, fuelBurn = (304 / 60).toInt, speed = 337, range = 1480, price = 12_540_000, lifespan = 35 * 52, constructionTime = 2, Manufacturer("Bombardier", countryCode = "CA"), runwayRequirement = 366, imageUrl = ""),
    Model("Bombardier CRJ1000", "Bombardier CRJ", capacity = 104, fuelBurn = (1809 / 60).toInt, speed = 828, range = 3056, price = 63_600_000, lifespan = 35 * 52, constructionTime = 6, Manufacturer("Bombardier", countryCode = "CA"), runwayRequirement = 1876, imageUrl = ""),
    Model("Sukhoi Superjet 100-95B", "Sukhoi Superjet", capacity = 108, fuelBurn = (1739 / 60).toInt, speed = 860, range = 3279, price = 62_000_000, lifespan = 35 * 52, constructionTime = 6, Manufacturer("JSC Sukhoi", countryCode = "RU"), runwayRequirement = 1731, imageUrl = ""),
    Model("ATR 72-500", "ATR-Regional", capacity = 68, fuelBurn = (1064 / 60).toInt, speed = 510, range = 1430, price = 28_100_000, lifespan = 35 * 52, constructionTime = 4, Manufacturer("ATR", countryCode = "FR"), runwayRequirement = 1296, imageUrl = ""),
    Model("Boeing 787-8 Dreamliner", "Boeing 787", capacity = 366, fuelBurn = (4852 / 60).toInt, speed = 880, range = 12874, price = 198_000_000, lifespan = 35 * 52, constructionTime = 28, Manufacturer("Boeing", countryCode = "US"), runwayRequirement = 3048, imageUrl = ""),
    Model("Boeing 747-8i", "Boeing 747", capacity = 660, fuelBurn = (10280 / 60).toInt, speed = 880, range = 14310, price = 515_100_000, lifespan = 35 * 52, constructionTime = 40, Manufacturer("Boeing", countryCode = "US"), runwayRequirement = 3190, imageUrl = ""),
    Model("Sukhoi Superjet 100-95LR", "Sukhoi Superjet", capacity = 90, fuelBurn = (1764 / 60).toInt, speed = 860, range = 4620, price = 61_500_000, lifespan = 35 * 52, constructionTime = 6, Manufacturer("JSC Sukhoi", countryCode = "RU"), runwayRequirement = 2052, imageUrl = ""),
    Model("Boeing 787-9 Dreamliner", "Boeing 787", capacity = 404, fuelBurn = (5614 / 60).toInt, speed = 880, range = 13530, price = 337_100_000, lifespan = 35 * 52, constructionTime = 30, Manufacturer("Boeing", countryCode = "US"), runwayRequirement = 3119, imageUrl = ""),
    Model("Airbus A350-900", "Airbus A350", capacity = 420, fuelBurn = (5907 / 60).toInt, speed = 871, range = 14174, price = 401_200_000, lifespan = 35 * 52, constructionTime = 30, Manufacturer("Airbus", countryCode = "NL"), runwayRequirement = 2800, imageUrl = ""),
    Model("Airbus A320neo", "Airbus A320", capacity = 195, fuelBurn = (2242 / 60).toInt, speed = 833, range = 6500, price = 141_600_000, lifespan = 35 * 52, constructionTime = 18, Manufacturer("Airbus", countryCode = "NL"), runwayRequirement = 2250, imageUrl = ""),
    Model("Irkut MC-21-300", "Irkut MC-21", capacity = 211, fuelBurn = (2932 / 60).toInt, speed = 870, range = 5300, price = 123_000_000, lifespan = 35 * 52, constructionTime = 18, Manufacturer("Irkut", countryCode = "RU"), runwayRequirement = 2410, imageUrl = ""),
    Model("Comac C919-700", "Comac C919", capacity = 192, fuelBurn = (2860 / 60).toInt, speed = 834, range = 3800, price = 99_000_000, lifespan = 35 * 52, constructionTime = 18, Manufacturer("COMAC", countryCode = "CN"), runwayRequirement = 2000, imageUrl = ""),
    Model("Bombardier CS100", "Bombardier CS", capacity = 133, fuelBurn = (1709 / 60).toInt, speed = 829, range = 5714, price = 85_700_000, lifespan = 35 * 52, constructionTime = 12, Manufacturer("Bombardier", countryCode = "CA"), runwayRequirement = 1463, imageUrl = ""),
    Model("Bombardier CS300", "Bombardier CS", capacity = 160, fuelBurn = (2152 / 60).toInt, speed = 829, range = 6112, price = 97_900_000, lifespan = 35 * 52, constructionTime = 12, Manufacturer("Bombardier", countryCode = "CA"), runwayRequirement = 1890, imageUrl = ""),
    Model("Irkut MC-21-200", "Irkut MC-21", capacity = 165, fuelBurn = (2559 / 60).toInt, speed = 870, range = 5950, price = 120_000_000, lifespan = 35 * 52, constructionTime = 12, Manufacturer("Irkut", countryCode = "RU"), runwayRequirement = 2160, imageUrl = ""),
    Model("Airbus A321neo", "Airbus A320", capacity = 240, fuelBurn = (2748 / 60).toInt, speed = 833, range = 6850, price = 165_800_000, lifespan = 35 * 52, constructionTime = 18, Manufacturer("Airbus", countryCode = "NL"), runwayRequirement = 2165, imageUrl = ""),
    Model("Boeing 737 MAX 8", "Boeing 737", capacity = 189, fuelBurn = (2220 / 60).toInt, speed = 829, range = 6750, price = 155_600_000, lifespan = 35 * 52, constructionTime = 12, Manufacturer("Boeing", countryCode = "US"), runwayRequirement = 2577, imageUrl = ""),
    Model("Comac C919-700ER", "Comac C919", capacity = 168, fuelBurn = (2478 / 60).toInt, speed = 834, range = 5576, price = 104_500_000, lifespan = 35 * 52, constructionTime = 12, Manufacturer("COMAC", countryCode = "CN"), runwayRequirement = 2125, imageUrl = ""),
    Model("Irkut MC-21-100", "Irkut MC-21", capacity = 132, fuelBurn = (2105 / 60).toInt, speed = 870, range = 6400, price = 108_500_000, lifespan = 35 * 52, constructionTime = 12, Manufacturer("Irkut", countryCode = "RU"), runwayRequirement = 1840, imageUrl = ""),
    Model("Airbus A350-1000", "Airbus A350", capacity = 460, fuelBurn = (6739 / 60).toInt, speed = 871, range = 14980, price = 470_000_000, lifespan = 35 * 52, constructionTime = 30, Manufacturer("Airbus", countryCode = "NL"), runwayRequirement = 2980, imageUrl = ""),
    Model("Boeing 737 MAX 9", "Boeing 737", capacity = 220, fuelBurn = (2706 / 60).toInt, speed = 829, range = 5751, price = 157_000_000, lifespan = 35 * 52, constructionTime = 18, Manufacturer("Boeing", countryCode = "US"), runwayRequirement = 2678, imageUrl = ""),
    Model("Embraer E190-E2", "Embraer E-Jet E2", capacity = 114, fuelBurn = (1881 / 60).toInt, speed = 829, range = 5278, price = 78_000_000, lifespan = 35 * 52, constructionTime = 8, Manufacturer("Embraer", countryCode = "BR"), runwayRequirement = 1800, imageUrl = ""),
    Model("Boeing 787-10 Dreamliner", "Boeing 787", capacity = 420, fuelBurn = (5715 / 60).toInt, speed = 880, range = 11742, price = 402_000_000, lifespan = 35 * 52, constructionTime = 30, Manufacturer("Boeing", countryCode = "US"), runwayRequirement = 3010, imageUrl = ""),
    Model("Airbus A321neoLR", "Airbus A320", capacity = 240, fuelBurn = (2844 / 60).toInt, speed = 833, range = 7400, price = 166_000_000, lifespan = 35 * 52, constructionTime = 18, Manufacturer("Airbus", countryCode = "NL"), runwayRequirement = 2315, imageUrl = ""),
    Model("Airbus A350-900ULR", "Airbus A350", capacity = 360, fuelBurn = (5700 / 60).toInt, speed = 871, range = 18265, price = 407_000_000, lifespan = 35 * 52, constructionTime = 28, Manufacturer("Airbus", countryCode = "NL"), runwayRequirement = 2800, imageUrl = ""),
    Model("Airbus A330-900neo", "Airbus A330", capacity = 420, fuelBurn = (5321 / 60).toInt, speed = 860, range = 11589, price = 380_000_000, lifespan = 35 * 52, constructionTime = 30, Manufacturer("Airbus", countryCode = "NL"), runwayRequirement = 3048, imageUrl = ""),
    Model("Airbus A319neo", "Airbus A320", capacity = 160, fuelBurn = (2016 / 60).toInt, speed = 833, range = 6950, price = 130_100_000, lifespan = 35 * 52, constructionTime = 12, Manufacturer("Airbus", countryCode = "NL"), runwayRequirement = 1850, imageUrl = ""),
    Model("Boeing 737 MAX 7", "Boeing 737", capacity = 172, fuelBurn = (2347 / 60).toInt, speed = 829, range = 7083, price = 127_800_000, lifespan = 35 * 52, constructionTime = 18, Manufacturer("Boeing", countryCode = "US"), runwayRequirement = 2100, imageUrl = ""),
    Model("Embraer E195-E2", "Embraer E-Jet E2", capacity = 146, fuelBurn = (1885 / 60).toInt, speed = 829, range = 4800, price = 88_000_000, lifespan = 35 * 52, constructionTime = 12, Manufacturer("Embraer", countryCode = "BR"), runwayRequirement = 1970, imageUrl = ""),
    Model("Comac C919-600", "Comac C919", capacity = 153, fuelBurn = (2256 / 60).toInt, speed = 834, range = 3750, price = 99_000_000, lifespan = 35 * 52, constructionTime = 12, Manufacturer("COMAC", countryCode = "CN"), runwayRequirement = 1890, imageUrl = ""),
    Model("Airbus A330-800neo", "Airbus A330", capacity = 390, fuelBurn = (5200 / 60).toInt, speed = 860, range = 13591, price = 333_200_000, lifespan = 35 * 52, constructionTime = 30, Manufacturer("Airbus", countryCode = "NL"), runwayRequirement = 3151, imageUrl = ""),
    Model("Boeing 737 MAX 10", "Boeing 737", capacity = 244, fuelBurn = (2952 / 60).toInt, speed = 829, range = 5740, price = 134_900_000, lifespan = 35 * 52, constructionTime = 18, Manufacturer("Boeing", countryCode = "US"), runwayRequirement = 2500, imageUrl = ""),
    Model("Boeing 777-9", "Boeing 777", capacity = 528, fuelBurn = (8000 / 60).toInt, speed = 875, range = 15500, price = 546_000_000, lifespan = 35 * 52, constructionTime = 40, Manufacturer("Boeing", countryCode = "US"), runwayRequirement = 3186, imageUrl = ""),
    Model("Comac C919-800", "Comac C919", capacity = 240, fuelBurn = (3432 / 60).toInt, speed = 834, range = 5560, price = 121_000_000, lifespan = 35 * 52, constructionTime = 18, Manufacturer("COMAC", countryCode = "CN"), runwayRequirement = 2650, imageUrl = ""),
    Model("Boeing 737 MAX 8-200", "Boeing 737", capacity = 200, fuelBurn = (2480 / 60).toInt, speed = 829, range = 6267, price = 160_000_000, lifespan = 35 * 52, constructionTime = 18, Manufacturer("Boeing", countryCode = "US"), runwayRequirement = 2577, imageUrl = ""),
    Model("Embraer E175-E2", "Embraer E-Jet E2", capacity = 88, fuelBurn = (1452 / 60).toInt, speed = 829, range = 3745, price = 61_500_000, lifespan = 35 * 52, constructionTime = 6, Manufacturer("Embraer", countryCode = "BR"), runwayRequirement = 1450, imageUrl = ""),
    Model("Boeing 777-8", "Boeing 777", capacity = 440, fuelBurn = (6100 / 60).toInt, speed = 875, range = 16840, price = 525_900_000, lifespan = 35 * 52, constructionTime = 40, Manufacturer("Boeing", countryCode = "US"), runwayRequirement = 3046, imageUrl = ""),
    Model("Airbus A321neoXLR", "Airbus A320", capacity = 236, fuelBurn = (3240 / 60).toInt, speed = 833, range = 8700, price = 166_000_000, lifespan = 35 * 52, constructionTime = 18, Manufacturer("Airbus", countryCode = "NL"), runwayRequirement = 2450, imageUrl = ""))
  val modelByName = models.map { model => (model.name, model) }.toMap
}