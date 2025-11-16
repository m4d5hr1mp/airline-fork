package com.patson.model.airplane

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

  val airplaneType: Type = {
    if (speed > SUPERSONIC_SPEED_THRESHOLD) {
      SUPERSONIC
    } else {
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

  /* Turnaround Time Logic:
  For Light aircraft:       from 20+(7/5) = 21.4    to 20+(19/5)=24.75
  For Small aircraft:       from 25+(20/5)= 29.0    to 20+(50/5)=30.0
  For Regional aircraft:    from 25+(51/7)= 32.0    to 25+(124/7)=42.8
  For Medium (Mainline):    from 30+(134/7)=49    to 30+(244/7)=65.0
  For Large (Small WB):     from 40+(250/3.5)=111.4 to 40+(360/3.5)=142.8
  For X-Large (B772):       from 50+(361/3)=170     to 50+(440/3)=196.6
  For Jumbo: 243-280-344    60+(550/3)=243, 60+(600/3)=260, 60+(660/3)=280, 60+(850/3)=343
  */
  private[this] val BASE_TURNAROUND_TIME = Map(
    LIGHT -> 20,
    SMALL -> 25,
    REGIONAL -> 25,
    MEDIUM -> 30,
    LARGE -> 40,
    X_LARGE -> 50,
    JUMBO -> 60,
    SUPERSONIC -> 50
  )

  val turnaroundTime: Int = (
    BASE_TURNAROUND_TIME(airplaneType) +
      (airplaneType match {
        case LIGHT      => (capacity / 5).toInt
        case SMALL      => (capacity / 5).toInt
        case REGIONAL   => (capacity / 7).toInt
        case MEDIUM     => (capacity / 7).toInt
        case LARGE      => (capacity / 3.5).toInt
        case X_LARGE    => (capacity / 3.5).toInt
        case JUMBO      => (capacity / 3).toInt
        case SUPERSONIC => (capacity / 2.5).toInt
      })
  )

  val airplaneTypeLabel: String = label(airplaneType)

  // Weekly fixed cost per aircraft, in USD (computed as per-seat rate multiplied by maximum certified capacity)
  val baseMaintenanceCost: Int = {
    val perSeatRate: Int = airplaneType match {
      case LIGHT => 100      // Based on light jet benchmarks (e.g., GA turbojet maintenance ~905 USD/hour, scaled for low complexity)
      case SMALL => 120      // Aligned with small regional data (e.g., FAA RJ ≤60 seats ~479 USD/hour)
      case REGIONAL => 140   // Reflects regional jet costs (e.g., FAA RJ >60 seats ~431 USD/hour)
      case MEDIUM => 150     // Matches narrow-body averages (e.g., A320/B737 ~718 USD/hour)
      case LARGE => 180      // Accounts for wide-body complexity (e.g., two-engine wide ~1,986 USD/hour)
      case X_LARGE => 200    // Escalated for larger wide-bodies (e.g., B777/A350 equivalents)
      case JUMBO => 220      // Higher for jumbo types (e.g., four-engine wide ~2,347 USD/hour)
      case SUPERSONIC => 300 // Premium based on Concorde's elevated maintenance ratios
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
    val LIGHT, SMALL, REGIONAL, MEDIUM, LARGE, X_LARGE, JUMBO, SUPERSONIC = Value

    val label = (airplaneType : Type) => { airplaneType match {
        case LIGHT => "Light"
        case SMALL => "Small"
        case REGIONAL => "Regional"
        case MEDIUM => "Medium"
        case LARGE => "Large"
        case X_LARGE => "Extra large"
        case JUMBO => "Jumbo"
        case SUPERSONIC => "Supersonic"
      }
    }
  }

  object Category extends Enumeration {
    type Category = Value
    val LIGHT, REGIONAL, MEDIUM, LARGE, SUPERSONIC = Value
    val grouping = Map(
      LIGHT -> List(Type.LIGHT, Type.SMALL),
      REGIONAL -> List(Type.REGIONAL),
      MEDIUM -> List(Type.MEDIUM),
      LARGE -> List(Type.LARGE, Type.X_LARGE, Type.JUMBO),
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
  val models = List(Model("Tupolev Tu-204-300", "Tupolev Tu-204", capacity = 210, fuelBurn = (2937 / 60).toInt, speed = 850, range = 6810, price = 70_000_000, lifespan = 25 * 52, constructionTime = 18, Manufacturer("UAC", countryCode = "RU"), runwayRequirement = 2050, imageUrl = "https://www.norebbo.com/tupolev-tu-204-100-blank-illustration-templates/"),
    Model("Tupolev Tu-204-100", "Tupolev Tu-204", capacity = 164, fuelBurn = (2705 / 60).toInt, speed = 850, range = 6000, price = 62_500_000, lifespan = 25 * 52, constructionTime = 16, Manufacturer("UAC", countryCode = "RU"), runwayRequirement = 1890, imageUrl = "https://www.norebbo.com/tupolev-tu-204-100-blank-illustration-templates/"),
    Model("Sukhoi Superjet 100-95LR",     "Sukhoi Superjet 100",      capacity = 90,    fuelBurn = (1764 / 60).toInt,   speed = 860,  range = 4620,   price = 45_000_000,   lifespan = 25 * 52, constructionTime = 8,   Manufacturer("UAC", countryCode = "RU"),                runwayRequirement = 2052, imageUrl = "https://www.norebbo.com/2016/02/sukhoi-ssj-100-blank-illustration-templates/"),
    Model("Sukhoi Superjet 100-95B",      "Sukhoi Superjet 100",      capacity = 108,   fuelBurn = (1739 / 60).toInt,   speed = 860,  range = 3279,   price = 40_000_000,   lifespan = 25 * 52, constructionTime = 8,   Manufacturer("UAC", countryCode = "RU"),                runwayRequirement = 1731, imageUrl = "https://www.norebbo.com/2016/02/sukhoi-ssj-100-blank-illustration-templates/"),
    Model("McDonnell Douglas MD-95",      "McDonnell Douglas MD-90",  capacity = 134,   fuelBurn = (1846 / 60).toInt,   speed = 816,  range = 2645,   price = 47_500_000,   lifespan = 25 * 52, constructionTime = 12,  Manufacturer("McDonnell Douglas", countryCode = "US"),  runwayRequirement = 2100, imageUrl = "https://www.norebbo.com/2017/06/boeing-717-200-blank-illustration-templates/"),
    Model("McDonnell Douglas MD-90-55",   "McDonnell Douglas MD-90",  capacity = 187,   fuelBurn = (2973 / 60).toInt,   speed = 814,  range = 5005,   price = 62_500_000,   lifespan = 25 * 52, constructionTime = 16,  Manufacturer("McDonnell Douglas", countryCode = "US"),  runwayRequirement = 2550, imageUrl = "https://www.norebbo.com/2018/02/mcdonnell-douglas-md-90-blank-illustration-templates/"),
    Model("McDonnell Douglas MD-90-40",   "McDonnell Douglas MD-90",  capacity = 172,   fuelBurn = (2717 / 60).toInt,   speed = 814,  range = 3537,   price = 57_500_000,   lifespan = 25 * 52, constructionTime = 16,  Manufacturer("McDonnell Douglas", countryCode = "US"),  runwayRequirement = 2300, imageUrl = "https://www.norebbo.com/2018/02/mcdonnell-douglas-md-90-blank-illustration-templates/"),
    Model("McDonnell Douglas MD-90-30ER", "McDonnell Douglas MD-90",  capacity = 153,   fuelBurn = (2478 / 60).toInt,   speed = 814,  range = 4426,   price = 52_500_000,   lifespan = 25 * 52, constructionTime = 12,  Manufacturer("McDonnell Douglas", countryCode = "US"),  runwayRequirement = 2134, imageUrl = "https://www.norebbo.com/2018/02/mcdonnell-douglas-md-90-blank-illustration-templates/"),
    Model("McDonnell Douglas MD-90-30",   "McDonnell Douglas MD-90",  capacity = 153,   fuelBurn = (2409 / 60).toInt,   speed = 814,  range = 3862,   price = 80_000_000,   lifespan = 25 * 52, constructionTime = 12,  Manufacturer("McDonnell Douglas", countryCode = "US"),  runwayRequirement = 2000, imageUrl = "https://www.norebbo.com/2018/02/mcdonnell-douglas-md-90-blank-illustration-templates/"),
    Model("Irkut MC-21-300",              "Irkut MC-21",              capacity = 211,   fuelBurn = (2932 / 60).toInt,   speed = 870,  range = 5300,   price = 90_000_000,   lifespan = 30 * 52, constructionTime = 18,  Manufacturer("UAC", countryCode = "RU"),                runwayRequirement = 2410, imageUrl = "https://www.norebbo.com/irkut-mc-21-300/"),
    Model("Irkut MC-21-200",              "Irkut MC-21",              capacity = 165,   fuelBurn = (2559 / 60).toInt,   speed = 870,  range = 5950,   price = 77_500_000,   lifespan = 30 * 52, constructionTime = 16,  Manufacturer("UAC", countryCode = "RU"),                runwayRequirement = 2160, imageUrl = "https://www.norebbo.com/irkut-mc-21-300/"),
    Model("Irkut MC-21-100",              "Irkut MC-21",              capacity = 132,   fuelBurn = (2105 / 60).toInt,   speed = 870,  range = 6400,   price = 60_000_000,   lifespan = 30 * 52, constructionTime = 12,  Manufacturer("UAC", countryCode = "RU"),                runwayRequirement = 1840, imageUrl = "https://www.norebbo.com/irkut-mc-21-300/"),
    Model("Ilyushin Il-96-400",           "Ilyushin Il-96",           capacity = 430,   fuelBurn = (9760 / 60).toInt,   speed = 871,  range = 11550,  price = 150_000_000,  lifespan = 25 * 52, constructionTime = 42,  Manufacturer("UAC", countryCode = "RU"),                runwayRequirement = 2780, imageUrl = ""),
    Model("Ilyushin Il-96-300",           "Ilyushin Il-96",           capacity = 300,   fuelBurn = (9644 / 60).toInt,   speed = 871,  range = 11500,  price = 125_000_000,  lifespan = 25 * 52, constructionTime = 28,  Manufacturer("UAC", countryCode = "RU"),                runwayRequirement = 2936, imageUrl = ""),
    Model("Gulfstream G650ER",            "Gulfstream",               capacity = 30,    fuelBurn = (1529 / 60).toInt,   speed = 904,  range = 13890,  price = 50_000_000,   lifespan = 40 * 52, constructionTime = 12,  Manufacturer("Gulfstream", countryCode = "US"),         runwayRequirement = 1920, imageUrl = "https://www.norebbo.com/gulfstream-g650er-template/"),
    Model("Fokker 70ER",                  "Fokker",                   capacity = 79,    fuelBurn = (1777 / 60).toInt,   speed = 845,  range = 3410,   price = 37_000_000,   lifespan = 25 * 52, constructionTime = 6,   Manufacturer("Fokker", countryCode = "NL"),             runwayRequirement = 1300, imageUrl = ""),
    Model("Fokker 70",                    "Fokker",                   capacity = 79,    fuelBurn = (1678 / 60).toInt,   speed = 845,  range = 2010,   price = 35_000_000,   lifespan = 25 * 52, constructionTime = 6,   Manufacturer("Fokker", countryCode = "NL"),             runwayRequirement = 1300, imageUrl = ""),
    Model("Fokker 50",                    "Fokker",                   capacity = 60,    fuelBurn = (912 / 60).toInt,    speed = 471,  range = 2400,   price = 22_500_000,   lifespan = 25 * 52, constructionTime = 4,   Manufacturer("Fokker", countryCode = "NL"),             runwayRequirement = 1350, imageUrl = ""),
    Model("Fokker 100",                   "Fokker",                   capacity = 109,   fuelBurn = (2000 / 60).toInt,   speed = 845,  range = 3170,   price = 37_500_000,   lifespan = 25 * 52, constructionTime = 8,   Manufacturer("Fokker", countryCode = "NL"),             runwayRequirement = 1621, imageUrl = "https://www.norebbo.com/2018/07/fokker-100-f-28-0100-blank-illustration-templates/"),
    Model("Embraer ERJ145XR",             "Embraer ERJ",              capacity = 50,    fuelBurn = (1200 / 60).toInt,   speed = 850,  range = 3700,   price = 22_500_000,   lifespan = 30 * 52, constructionTime = 2,   Manufacturer("Embraer", countryCode = "BR"),            runwayRequirement = 1720, imageUrl = "https://www.norebbo.com/2018/04/embraer-erj-145xr-blank-illustration-templates/"),
    Model("Embraer ERJ145",               "Embraer ERJ",              capacity = 50,    fuelBurn = (1100 / 60).toInt,   speed = 850,  range = 2870,   price = 25_000_000,   lifespan = 30 * 52, constructionTime = 2,   Manufacturer("Embraer", countryCode = "BR"),            runwayRequirement = 1560, imageUrl = "https://www.norebbo.com/2018/04/embraer-erj-145-blank-illustration-templates/"),
    Model("Embraer ERJ140",               "Embraer ERJ",              capacity = 44,    fuelBurn = (1056 / 60).toInt,   speed = 850,  range = 3000,   price = 20_000_000,   lifespan = 30 * 52, constructionTime = 2,   Manufacturer("Embraer",countryCode = "BR"),             runwayRequirement = 1970, imageUrl = "https://www.norebbo.com/2018/05/embraer-erj-140-blank-illustration-templates/"),
    Model("Embraer ERJ135",               "Embraer ERJ",              capacity = 37,    fuelBurn = (907 / 60).toInt,    speed = 850,  range = 3240,   price = 17_500_000,   lifespan = 30 * 52, constructionTime = 2,   Manufacturer("Embraer",countryCode = "BR"),             runwayRequirement = 1580, imageUrl = "https://www.norebbo.com/2018/05/embraer-erj-135-blank-illustration-templates/"),
    Model("Embraer EMB195",               "Embraer ERJ",              capacity = 124,   fuelBurn = (1891 / 60).toInt,   speed = 829,  range = 3334,   price = 55_900_000,   lifespan = 30 * 52, constructionTime = 8,   Manufacturer("Embraer",countryCode = "BR"),             runwayRequirement = 2080, imageUrl = "https://www.norebbo.com/2015/06/embraer-190-blank-illustration-templates/"),
    Model("Embraer EMB190",               "Embraer ERJ",              capacity = 108,   fuelBurn = (1809 / 60).toInt,   speed = 829,  range = 4445,   price = 50_000_000,   lifespan = 30 * 52, constructionTime = 8,   Manufacturer("Embraer",countryCode = "BR"),             runwayRequirement = 1890, imageUrl = "https://www.norebbo.com/2015/06/embraer-190-blank-illustration-templates/"),
    Model("Embraer EMB175",               "Embraer ERJ",              capacity = 88,    fuelBurn = (1518 / 60).toInt,   speed = 829,  range = 3521,   price = 42_500_000,   lifespan = 30 * 52, constructionTime = 6,   Manufacturer("Embraer", countryCode = "BR"),            runwayRequirement = 1612, imageUrl = "https://www.norebbo.com/embraer-erj-175-templates-with-the-new-style-winglets/"),
    Model("Embraer EMB170",               "Embraer ERJ",              capacity = 72,    fuelBurn = (1386 / 60).toInt,   speed = 829,  range = 3334,   price = 39_000_000,   lifespan = 30 * 52, constructionTime = 6,   Manufacturer("Embraer",countryCode = "BR"),             runwayRequirement = 1644, imageUrl = "https://www.norebbo.com/2015/10/embraer-erj-175-templates-with-the-new-style-winglets/"),
    Model("Embraer EMB120 Brasilia",      "Embraer ERJ",              capacity = 30,    fuelBurn = (412 / 60).toInt,    speed = 552,  range = 1750,   price = 17_000_000,   lifespan = 35 * 52, constructionTime = 0,   Manufacturer("Embraer",countryCode = "BR"),             runwayRequirement = 1420, imageUrl = "https://www.norebbo.com/2015/02/embraer-120-brasilia-blank-illustration-templates/"),
    Model("Embraer E195-E2",              "Embraer E-Jet E2",         capacity = 146,   fuelBurn = (1885 / 60).toInt,   speed = 833,  range = 4800,   price = 80_000_000,   lifespan = 35 * 52, constructionTime = 12,  Manufacturer("Embraer",countryCode = "BR"),             runwayRequirement = 1970, imageUrl = "https://www.norebbo.com/2019/03/embraer-e195-e2-side-view/"),
    Model("Embraer E190-E2",              "Embraer E-Jet E2",         capacity = 114,   fuelBurn = (1881 / 60).toInt,   speed = 833,  range = 5278,   price = 57_000_000,   lifespan = 35 * 52, constructionTime = 8,   Manufacturer("Embraer",countryCode = "BR"),             runwayRequirement = 1800, imageUrl = "https://www.norebbo.com/2019/03/e190-e2-blank-side-view/"),
    Model("Embraer E175-E2",              "Embraer E-Jet E2",         capacity = 88,    fuelBurn = (1452 / 60).toInt,   speed = 833,  range = 3735,   price = 55_000_000,   lifespan = 35 * 52, constructionTime = 6,   Manufacturer("Embraer",countryCode = "BR"),             runwayRequirement = 1450, imageUrl = "https://www.norebbo.com/2019/03/e175-e2-side-view/"),
    Model("Comac C919-800",               "Comac C919",               capacity = 240,   fuelBurn = (3432 / 60).toInt,   speed = 834,  range = 5560,   price = 92_500_000,   lifespan = 30 * 52, constructionTime = 18,  Manufacturer("COMAC",countryCode = "CN"),               runwayRequirement = 2650, imageUrl = "https://www.norebbo.com/comac-c919-side-view/"),
    Model("Comac C919-600",               "Comac C919",               capacity = 153,   fuelBurn = (2256 / 60).toInt,   speed = 834,  range = 3750,   price = 65_000_000,   lifespan = 30 * 52, constructionTime = 12,  Manufacturer("COMAC",countryCode = "CN"),               runwayRequirement = 1890, imageUrl = "https://www.norebbo.com/comac-c919-side-view/"),
    Model("Comac C919-100ER",             "Comac C919",               capacity = 168,   fuelBurn = (2478 / 60).toInt,   speed = 834,  range = 5576,   price = 85_000_000,   lifespan = 30 * 52, constructionTime = 16,  Manufacturer("COMAC",countryCode = "CN"),               runwayRequirement = 2125, imageUrl = "https://www.norebbo.com/comac-c919-side-view/"),
    Model("Comac C919-100",               "Comac C919",               capacity = 192,   fuelBurn = (2860 / 60).toInt,   speed = 834,  range = 4075,   price = 87_500_000,   lifespan = 30 * 52, constructionTime = 16,  Manufacturer("COMAC",countryCode = "CN"),               runwayRequirement = 2000, imageUrl = "https://www.norebbo.com/comac-c919-side-view/"),
    Model("Comac ARJ21",                  "Comac ARJ",                capacity = 90,    fuelBurn = (1912 / 60).toInt,   speed = 828,  range = 2876,   price = 45_000_000,   lifespan = 30 * 52, constructionTime = 6,   Manufacturer("COMAC",countryCode = "CN"),               runwayRequirement = 1760, imageUrl = ""),
    Model("Bombardier Q400",              "Bombardier DHC-8",         capacity = 96,    fuelBurn = (1248 / 60).toInt,   speed = 667,  range = 2040,   price = 35_000_000,   lifespan = 30 * 52, constructionTime = 8,   Manufacturer("Bombardier",countryCode = "CA"),          runwayRequirement = 1877, imageUrl = "https://www.norebbo.com/2015/08/bombardier-dhc-8-402-q400-blank-illustration-templates/"),
    Model("Bombardier Q300",              "Bombardier DHC-8",         capacity = 56,    fuelBurn = (770 / 60).toInt,    speed = 528,  range = 1500,   price = 25_000_000,   lifespan = 30 * 52, constructionTime = 4,   Manufacturer("Bombardier",countryCode = "CA"),          runwayRequirement = 1085, imageUrl = "https://www.norebbo.com/2018/05/de-havilland-dhc-8-300-blank-illustration-templates/"),
    Model("Bombardier DHC-8-400",         "Bombardier DHC-8",         capacity = 78,    fuelBurn = (1014 / 60).toInt,   speed = 667,  range = 2037,   price = 27_500_000,   lifespan = 30 * 52, constructionTime = 6,   Manufacturer("Bombardier",countryCode = "CA"),          runwayRequirement = 1277, imageUrl = "https://www.norebbo.com/2015/08/bombardier-dhc-8-402-q400-blank-illustration-templates/"),
    Model("Bombardier DHC-8-300",         "Bombardier DHC-8",         capacity = 56,    fuelBurn = (896 / 60).toInt,    speed = 528,  range = 1500,   price = 22_500_000,   lifespan = 30 * 52, constructionTime = 4,   Manufacturer("Bombardier",countryCode = "CA"),          runwayRequirement = 1085, imageUrl = "https://www.norebbo.com/2018/05/de-havilland-dhc-8-300-blank-illustration-templates/"),
    Model("Bombardier DHC-8-200",         "Bombardier DHC-8",         capacity = 40,    fuelBurn = (600 / 60).toInt,    speed = 463,  range = 2083,   price = 15_000_000,   lifespan = 30 * 52, constructionTime = 0,   Manufacturer("Bombardier",countryCode = "CA"),          runwayRequirement = 1067, imageUrl = "https://www.norebbo.com/2018/01/de-havilland-dhc-8-200-dash-8-blank-illustration-templates/"),
    Model("Bombardier DHC-6-400",         "Bombardier DHC-8",         capacity = 19,    fuelBurn = (304 / 60).toInt,    speed = 337,  range = 1480,   price = 8_000_000,    lifespan = 30 * 52, constructionTime = 0,   Manufacturer("Bombardier",countryCode = "CA"),          runwayRequirement = 366,  imageUrl = ""),
    Model("Bombardier CS300",             "Bombardier CS",            capacity = 160,   fuelBurn = (2152 / 60).toInt,   speed = 828,  range = 6112,   price = 97_500_000,   lifespan = 35 * 52, constructionTime = 16,  Manufacturer("Bombardier",countryCode = "CA"),          runwayRequirement = 1890, imageUrl = "https://www.norebbo.com/2016/02/bombardier-cs300-blank-illustration-templates/"),
    Model("Bombardier CS100",             "Bombardier CS",            capacity = 133,   fuelBurn = (1709 / 60).toInt,   speed = 828,  range = 5741,   price = 80_000_000,   lifespan = 35 * 52, constructionTime = 12,  Manufacturer("Bombardier",countryCode = "CA"),          runwayRequirement = 1463, imageUrl = "https://www.norebbo.com/2016/02/bombardier-cs100-blank-illustration-templates/"),
    Model("Bombardier CRJ900",            "Bombardier CRJ",           capacity = 90,    fuelBurn = (1912 / 60).toInt,   speed = 828,  range = 2876,   price = 45_000_000,   lifespan = 30 * 52, constructionTime = 6,   Manufacturer("Bombardier",countryCode = "CA"),          runwayRequirement = 1760, imageUrl = "https://www.norebbo.com/2016/07/bombardier-canadair-regional-jet-900-blank-illustration-templates/"),
    Model("Bombardier CRJ700",            "Bombardier CRJ",           capacity = 78,    fuelBurn = (1560 / 60).toInt,   speed = 828,  range = 2411,   price = 40_000_000,   lifespan = 30 * 52, constructionTime = 6,   Manufacturer("Bombardier",countryCode = "CA"),          runwayRequirement = 1516, imageUrl = "https://www.norebbo.com/2015/05/bombardier-canadair-regional-jet-700-blank-illustration-templates/"),
    Model("Bombardier CRJ200",            "Bombardier CRJ",           capacity = 50,    fuelBurn = (1125 / 60).toInt,   speed = 786,  range = 3150,   price = 25_500_000,   lifespan = 30 * 52, constructionTime = 2,   Manufacturer("Bombardier",countryCode = "CA"),          runwayRequirement = 1770, imageUrl = "https://www.norebbo.com/2015/04/bombardier-canadair-regional-jet-200-blank-illustration-templates/"),
    Model("Bombardier CRJ1000",           "Bombardier CRJ",           capacity = 104,   fuelBurn = (1809 / 60).toInt,   speed = 828,  range = 3004,   price = 47_500_000,   lifespan = 30 * 52, constructionTime = 8,   Manufacturer("Bombardier",countryCode = "CA"),          runwayRequirement = 1876, imageUrl = "https://www.norebbo.com/2019/06/bombardier-crj-1000-side-view/"),
    Model("Boeing 787-9 Dreamliner",      "Boeing 787",               capacity = 404,   fuelBurn = (5614 / 60).toInt,   speed = 880,  range = 13530,  price = 290_000_000,  lifespan = 35 * 52, constructionTime = 42,  Manufacturer("Boeing", countryCode = "US"),             runwayRequirement = 3119, imageUrl = "https://www.norebbo.com/2014/04/boeing-787-9-blank-illustration-templates/"),
    Model("Boeing 787-8 Dreamliner",      "Boeing 787",               capacity = 366,   fuelBurn = (4852 / 60).toInt,   speed = 880,  range = 12874,  price = 255_000_000,  lifespan = 35 * 52, constructionTime = 36,  Manufacturer("Boeing",countryCode = "US"),              runwayRequirement = 3048, imageUrl = "https://www.norebbo.com/2013/02/boeing-787-8-blank-illustration-templates/"),
    Model("Boeing 787-10 Dreamliner",     "Boeing 787",               capacity = 420,   fuelBurn = (5717 / 60).toInt,   speed = 880,  range = 11742,  price = 320_000_000,  lifespan = 35 * 52, constructionTime = 42,  Manufacturer("Boeing",countryCode = "US"),              runwayRequirement = 3010, imageUrl = "https://www.norebbo.com/2017/06/boeing-787-10-blank-illustration-templates/"),
    Model("Boeing 777-9",                 "Boeing 777",               capacity = 528,   fuelBurn = (8000 / 60).toInt,   speed = 875,  range = 15500,  price = 425_000_000,  lifespan = 35 * 52, constructionTime = 56,  Manufacturer("Boeing",countryCode = "US"),              runwayRequirement = 3048, imageUrl = "https://www.norebbo.com/2019/12/boeing-777-9-side-view/"),
    Model("Boeing 777-8",                 "Boeing 777",               capacity = 440,   fuelBurn = (6100 / 60).toInt,   speed = 875,  range = 16840,  price = 380_000_000,  lifespan = 35 * 52, constructionTime = 42,  Manufacturer("Boeing",countryCode = "US"),              runwayRequirement = 3050, imageUrl = "https://www.norebbo.com/2019/12/boeing-777-8-side-view/"),
    Model("Boeing 777-300ER",             "Boeing 777",               capacity = 528,   fuelBurn = (8416 / 60).toInt,   speed = 875,  range = 15017,  price = 415_000_000,  lifespan = 30 * 52, constructionTime = 56,  Manufacturer("Boeing",countryCode = "US"),              runwayRequirement = 3046, imageUrl = "https://www.norebbo.com/2014/03/boeing-777-300-blank-illustration-templates/"),
    Model("Boeing 777-300",               "Boeing 777",               capacity = 528,   fuelBurn = (7547 / 60).toInt,   speed = 875,  range = 11387,  price = 400_000_000,  lifespan = 30 * 52, constructionTime = 56,  Manufacturer("Boeing",countryCode = "US"),              runwayRequirement = 3186, imageUrl = "https://www.norebbo.com/2014/03/boeing-777-300-blank-illustration-templates/"),
    Model("Boeing 777-200LR",             "Boeing 777",               capacity = 420,   fuelBurn = (7873 / 60).toInt,   speed = 875,  range = 17305,  price = 370_000_000,  lifespan = 30 * 52, constructionTime = 42,  Manufacturer("Boeing",countryCode = "US"),              runwayRequirement = 2805, imageUrl = "https://www.norebbo.com/2012/12/boeing-777-200-blank-illustration-templates/"),
    Model("Boeing 777-200ER",             "Boeing 777",               capacity = 420,   fuelBurn = (7092 / 60).toInt,   speed = 875,  range = 13080,  price = 350_000_000,  lifespan = 30 * 52, constructionTime = 42,  Manufacturer("Boeing",countryCode = "US"),              runwayRequirement = 2890, imageUrl = "https://www.norebbo.com/2012/12/boeing-777-200-blank-illustration-templates/"),
    Model("Boeing 777-200",               "Boeing 777",               capacity = 420,   fuelBurn = (6495 / 60).toInt,   speed = 875,  range = 9574,   price = 300_000_000,  lifespan = 30 * 52, constructionTime = 42,  Manufacturer("Boeing",countryCode = "US"),              runwayRequirement = 2561, imageUrl = "https://www.norebbo.com/2012/12/boeing-777-200-blank-illustration-templates/"),
    Model("Boeing 767-400ER",             "Boeing 767",               capacity = 375,   fuelBurn = (5934 / 60).toInt,   speed = 858,  range = 10418,  price = 225_000_000,  lifespan = 30 * 52, constructionTime = 42,  Manufacturer("Boeing",countryCode = "US"),              runwayRequirement = 3171, imageUrl = "https://www.norebbo.com/2014/07/boeing-767-400-blank-illustration-templates/"),
    Model("Boeing 767-300ER",             "Boeing 767",               capacity = 290,   fuelBurn = (5416 / 60).toInt,   speed = 858,  range = 11484,  price = 195_000_000,  lifespan = 30 * 52, constructionTime = 28,  Manufacturer("Boeing",countryCode = "US"),              runwayRequirement = 2712, imageUrl = "https://www.norebbo.com/2014/07/boeing-767-300-blank-illustration-templates/"),
    Model("Boeing 767-300",               "Boeing 767",               capacity = 290,   fuelBurn = (5207 / 60).toInt,   speed = 858,  range = 7883,   price = 180_000_000,  lifespan = 30 * 52, constructionTime = 28,  Manufacturer("Boeing",countryCode = "US"),              runwayRequirement = 2418, imageUrl = "https://www.norebbo.com/2014/07/boeing-767-300-blank-illustration-templates/"),
    Model("Boeing 767-200ER",             "Boeing 767",               capacity = 250,   fuelBurn = (4565 / 60).toInt,   speed = 858,  range = 10042,  price = 175_000_000,  lifespan = 30 * 52, constructionTime = 28,  Manufacturer("Boeing", countryCode = "US"),             runwayRequirement = 2290, imageUrl = "https://www.norebbo.com/2014/07/boeing-767-200-blank-illustration-templates/"),
    Model("Boeing 767-200",               "Boeing 767",               capacity = 250,   fuelBurn = (4425 / 60).toInt,   speed = 858,  range = 6230,   price = 160_000_000,  lifespan = 30 * 52, constructionTime = 28,  Manufacturer("Boeing",countryCode = "US"),              runwayRequirement = 1829, imageUrl = "https://www.norebbo.com/2014/07/boeing-767-200-blank-illustration-templates/"),
    Model("Boeing 757-300",               "Boeing 757",               capacity = 279,   fuelBurn = (4505 / 60).toInt,   speed = 838,  range = 6681,   price = 150_000_000,  lifespan = 30 * 52, constructionTime = 28,  Manufacturer("Boeing",countryCode = "US"),              runwayRequirement = 2618, imageUrl = "https://www.norebbo.com/2017/03/boeing-757-300-blank-illustration-templates/"),
    Model("Boeing 757-200ER",             "Boeing 757",               capacity = 239,   fuelBurn = (3980 / 60).toInt,   speed = 838,  range = 9240,   price = 120_000_000,  lifespan = 30 * 52, constructionTime = 24,  Manufacturer("Boeing",countryCode = "US"),              runwayRequirement = 2890, imageUrl = "https://www.norebbo.com/2015/01/boeing-757-200-blank-illustration-templates/"),
    Model("Boeing 757-200",               "Boeing 757",               capacity = 239,   fuelBurn = (3781 / 60).toInt,   speed = 838,  range = 7250,   price = 125_000_000,  lifespan = 30 * 52, constructionTime = 18,  Manufacturer("Boeing",countryCode = "US"),              runwayRequirement = 2620, imageUrl = "https://www.norebbo.com/2015/01/boeing-757-200-blank-illustration-templates/"),
    Model("Boeing 747-8i",                "Boeing 747",               capacity = 660,   fuelBurn = (10280 / 60).toInt,  speed = 880,  range = 14310,  price = 420_000_000,  lifespan = 35 * 52, constructionTime = 60,  Manufacturer("Boeing",countryCode = "US"),              runwayRequirement = 3190, imageUrl = "https://www.norebbo.com/2015/12/boeing-747-8i-blank-illustration-templates/"),
    Model("Boeing 747-400ER",             "Boeing 747",               capacity = 634,   fuelBurn = (11870 / 60).toInt,  speed = 880,  range = 14460,  price = 375_000_000,  lifespan = 35 * 52, constructionTime = 60,  Manufacturer("Boeing",countryCode = "US"),              runwayRequirement = 3400, imageUrl = "https://www.norebbo.com/2013/09/boeing-747-400-blank-illustration-templates/"),
    Model("Boeing 747-400",               "Boeing 747",               capacity = 634,   fuelBurn = (11287 / 60).toInt,  speed = 880,  range = 13632,  price = 360_000_000,  lifespan = 30 * 52, constructionTime = 60,  Manufacturer("Boeing",countryCode = "US"),              runwayRequirement = 3315, imageUrl = "https://www.norebbo.com/2013/09/boeing-747-400-blank-illustration-templates/"),
    Model("Boeing 747-300",               "Boeing 747",               capacity = 608,   fuelBurn = (12270 / 60).toInt,  speed = 880,  range = 10976,  price = 305_000_000,  lifespan = 30 * 52, constructionTime = 60,  Manufacturer("Boeing",countryCode = "US"),              runwayRequirement = 3300, imageUrl = "https://www.norebbo.com/2019/10/boeing-747-300-side-view/"),
    Model("Boeing 747-200",               "Boeing 747",               capacity = 516,   fuelBurn = (11563 / 60).toInt,  speed = 880,  range = 10200,  price = 300_000_000,  lifespan = 30 * 52, constructionTime = 56,  Manufacturer("Boeing",countryCode = "US"),              runwayRequirement = 3347, imageUrl = "https://www.norebbo.com/2019/08/boeing-747-200-side-view/"),
    Model("Boeing 737-900ER",             "Boeing 737",               capacity = 220,   fuelBurn = (3063 / 60).toInt,   speed = 828,  range = 5756,   price = 100_000_000,  lifespan = 30 * 52, constructionTime = 18,  Manufacturer("Boeing",countryCode = "US"),              runwayRequirement = 2919, imageUrl = "https://www.norebbo.com/2014/08/boeing-737-900-blank-illustration-templates/"),
    Model("Boeing 737-900",               "Boeing 737",               capacity = 198,   fuelBurn = (2920 / 60).toInt,   speed = 828,  range = 5556,   price = 95_000_000,   lifespan = 30 * 52, constructionTime = 16,  Manufacturer("Boeing",countryCode = "US"),              runwayRequirement = 2179, imageUrl = "https://www.norebbo.com/2014/08/boeing-737-900-blank-illustration-templates/"),
    Model("Boeing 737-800",               "Boeing 737",               capacity = 184,   fuelBurn = (2484 / 60).toInt,   speed = 828,  range = 5840,   price = 90_000_000,   lifespan = 30 * 52, constructionTime = 16,  Manufacturer("Boeing",countryCode = "US"),              runwayRequirement = 1903, imageUrl = "https://www.norebbo.com/2012/11/boeing-737-800-blank-illustration-templates/"),
    Model("Boeing 737-700ER",             "Boeing 737",               capacity = 149,   fuelBurn = (2070 / 60).toInt,   speed = 828,  range = 10200,  price = 85_000_000,   lifespan = 30 * 52, constructionTime = 12,  Manufacturer("Boeing",countryCode = "US"),              runwayRequirement = 2196, imageUrl = "https://www.norebbo.com/2014/04/boeing-737-700-blank-illustration-templates/"),
    Model("Boeing 737-700",               "Boeing 737",               capacity = 149,   fuelBurn = (1996 / 60).toInt,   speed = 828,  range = 6232,   price = 75_000_000,   lifespan = 30 * 52, constructionTime = 12,  Manufacturer("Boeing",countryCode = "US"),              runwayRequirement = 1710, imageUrl = "https://www.norebbo.com/2014/04/boeing-737-700-blank-illustration-templates/"),
    Model("Boeing 737-600",               "Boeing 737",               capacity = 132,   fuelBurn = (1953 / 60).toInt,   speed = 828,  range = 5650,   price = 62_500_000,   lifespan = 30 * 52, constructionTime = 12,  Manufacturer("Boeing",countryCode = "US"),              runwayRequirement = 2000, imageUrl = "https://www.norebbo.com/2018/09/boeing-737-600-blank-illustration-templates/"),
    Model("Boeing 737-500",               "Boeing 737 Classic",       capacity = 132,   fuelBurn = (2138 / 60).toInt,   speed = 796,  range = 5650,   price = 52_500_000,   lifespan = 25 * 52, constructionTime = 12,  Manufacturer("Boeing",countryCode = "US"),              runwayRequirement = 1830, imageUrl = "https://www.norebbo.com/2018/09/boeing-737-500-blank-illustration-templates-with-and-without-blended-winglets/"),
    Model("Boeing 737-400",               "Boeing 737 Classic",       capacity = 188,   fuelBurn = (2932 / 60).toInt,   speed = 796,  range = 5184,   price = 60_000_000,   lifespan = 25 * 52, constructionTime = 16,  Manufacturer("Boeing",countryCode = "US"),              runwayRequirement = 2540, imageUrl = "https://www.norebbo.com/2018/09/boeing-737-400-blank-illustration-templates/"),
    Model("Boeing 737-300",               "Boeing 737 Classic",       capacity = 149,   fuelBurn = (2272 / 60).toInt,   speed = 796,  range = 5443,   price = 49_000_000,   lifespan = 25 * 52, constructionTime = 12,  Manufacturer("Boeing",countryCode = "US"),              runwayRequirement = 1940, imageUrl = "https://www.norebbo.com/2018/09/boeing-737-300-blank-illustration-templates/"),
    Model("Boeing 737 MAX 9",             "Boeing 737",               capacity = 220,   fuelBurn = (2706 / 60).toInt,   speed = 830,  range = 5751,   price = 115_000_000,  lifespan = 35 * 52, constructionTime = 18,  Manufacturer("Boeing",countryCode = "US"),              runwayRequirement = 3139, imageUrl = "https://www.norebbo.com/2018/05/boeing-737-9-max-blank-illustration-templates/"),
    Model("Boeing 737 MAX 8",             "Boeing 737",               capacity = 189,   fuelBurn = (2220 / 60).toInt,   speed = 830,  range = 6750,   price = 110_000_000,  lifespan = 35 * 52, constructionTime = 16,  Manufacturer("Boeing",countryCode = "US"),              runwayRequirement = 2577, imageUrl = "https://www.norebbo.com/2016/07/boeing-737-max-8-blank-illustration-templates/"),
    Model("Boeing 737 MAX 8-200",         "Boeing 737",               capacity = 200,   fuelBurn = (2480 / 60).toInt,   speed = 830,  range = 6267,   price = 115_000_000,  lifespan = 35 * 52, constructionTime = 18,  Manufacturer("Boeing",countryCode = "US"),              runwayRequirement = 2577, imageUrl = "https://www.norebbo.com/2016/07/boeing-737-max-8-blank-illustration-templates/"),
    Model("Boeing 737 MAX 7",             "Boeing 737",               capacity = 172,   fuelBurn = (2347 / 60).toInt,   speed = 830,  range = 7130,   price = 105_000_000,  lifespan = 35 * 52, constructionTime = 16,  Manufacturer("Boeing",countryCode = "US"),              runwayRequirement = 2100, imageUrl = "https://www.norebbo.com/2016/07/boeing-737-max-7-blank-illustration-templates/"),
    Model("Boeing 737 MAX 10",            "Boeing 737",               capacity = 244,   fuelBurn = (2952 / 60).toInt,   speed = 830,  range = 5740,   price = 130_000_000,  lifespan = 35 * 52, constructionTime = 18,  Manufacturer("Boeing",countryCode = "US"),              runwayRequirement = 2500, imageUrl = "https://www.norebbo.com/2019/01/737-10-max-side-view/"),
    Model("Avro RJ85",                    "Avro RJ",                  capacity = 100,   fuelBurn = (1820 / 60).toInt,   speed = 720,  range = 2095,   price = 40_000_000,   lifespan = 25 * 52, constructionTime = 8,   Manufacturer("British Aerospace",countryCode = "GB"),   runwayRequirement = 1390, imageUrl = "https://www.norebbo.com/2018/11/british-aerospace-bae-146-200-avro-rj85-blank-illustration-templates/"),
    Model("Avro RJ70" ,                   "Avro RJ",                  capacity = 82,    fuelBurn = (1551 / 60).toInt,   speed = 720,  range = 3870,   price = 35_500_000,   lifespan = 25 * 52, constructionTime = 6,   Manufacturer("British Aerospace",countryCode = "GB"),   runwayRequirement = 1095, imageUrl = "https://www.norebbo.com/2018/11/british-aerospace-bae-146-200-avro-rj85-blank-illustration-templates/"),
    Model("Avro RJ100",                   "Avro RJ",                  capacity = 112,   fuelBurn = (1932 / 60).toInt,   speed = 720,  range = 3340,   price = 42_500_000,   lifespan = 25 * 52, constructionTime = 8,   Manufacturer("British Aerospace",countryCode = "GB"),   runwayRequirement = 1535, imageUrl = "https://www.norebbo.com/2018/11/british-aerospace-bae-146-200-avro-rj85-blank-illustration-templates/"),
    Model("ATR 72-600",                   "ATR-Regional",             capacity = 78,    fuelBurn = (1209 / 60).toInt,   speed = 510,  range = 1370,   price = 26_000_000,   lifespan = 25 * 52, constructionTime = 6,   Manufacturer("ATR",             countryCode = "FR"),                 runwayRequirement = 1433, imageUrl = "https://www.norebbo.com/2017/04/atr-72-blank-illustration-templates/"),
    Model("ATR 72-500",                   "ATR-Regional",             capacity = 68,    fuelBurn = (1064 / 60).toInt,   speed = 510,  range = 1430,   price = 24_000_000,   lifespan = 25 * 52, constructionTime = 6,   Manufacturer("ATR",     countryCode = "FR"),                 runwayRequirement = 1296, imageUrl = "https://www.norebbo.com/2017/04/atr-72-blank-illustration-templates/"),
    Model("ATR 42-600",                   "ATR-Regional",             capacity = 52,    fuelBurn = (811 / 60).toInt,    speed = 556,  range = 1302,   price = 22_000_000,   lifespan = 25 * 52, constructionTime = 4,   Manufacturer("ATR",     countryCode = "FR"),                 runwayRequirement = 1165, imageUrl = "https://www.norebbo.com/2018/06/atr-42-blank-illustration-templates/"),
    Model("ATR 42-500",                   "ATR-Regional",             capacity = 48,    fuelBurn = (796 / 60).toInt,    speed = 535,  range = 1345,   price = 20_000_000,   lifespan = 25 * 52, constructionTime = 2,   Manufacturer("ATR",     countryCode = "FR"),                 runwayRequirement = 1278, imageUrl = "https://www.norebbo.com/2018/06/atr-42-blank-illustration-templates/"),
    Model("ATR 42-300",                   "ATR-Regional",             capacity = 40,    fuelBurn = (568 / 60).toInt,    speed = 494,  range = 850,    price = 16_000_000,   lifespan = 25 * 52, constructionTime = 0,   Manufacturer("ATR",     countryCode = "FR"),                 runwayRequirement = 1276, imageUrl = "https://www.norebbo.com/2018/06/atr-42-blank-illustration-templates/"),
    Model("Airbus A380-800",              "Airbus A380",              capacity = 820,   fuelBurn = (13493 / 60).toInt,  speed = 875,  range = 15199,  price = 450_000_000,  lifespan = 30 * 52, constructionTime = 64,  Manufacturer("Airbus",  countryCode = "NL"),              runwayRequirement = 3000, imageUrl = "https://www.norebbo.com/2013/06/airbus-a380-800-blank-illustration-templates/"),
    Model("Airbus A350-900ULR",           "Airbus A350",              capacity = 360,   fuelBurn = (5907 / 60).toInt,   speed = 871,  range = 18265,  price = 310_000_000,  lifespan = 35 * 52, constructionTime = 36,  Manufacturer("Airbus",  countryCode = "NL"),              runwayRequirement = 2800, imageUrl = "https://www.norebbo.com/2015/06/airbus-a350-800-blank-illustration-templates/"),
    Model("Airbus A350-900",              "Airbus A350",              capacity = 420,   fuelBurn = (5700 / 60).toInt,   speed = 871,  range = 14174,  price = 340_000_000,  lifespan = 35 * 52, constructionTime = 42,  Manufacturer("Airbus",  countryCode = "NL"),              runwayRequirement = 2800, imageUrl = "https://www.norebbo.com/2015/06/airbus-a350-800-blank-illustration-templates/"),
    Model("Airbus A350-1000",             "Airbus A350",              capacity = 460,   fuelBurn = (6739 / 60).toInt,   speed = 871,  range = 14980,  price = 365_000_000,  lifespan = 35 * 52, constructionTime = 42,  Manufacturer("Airbus",  countryCode = "NL"),              runwayRequirement = 2980, imageUrl = "https://www.norebbo.com/2015/11/airbus-a350-1000-blank-illustration-templates/"),
    Model("Airbus A340-600",              "Airbus A340",              capacity = 475,   fuelBurn = (8938 / 60).toInt,   speed = 871,  range = 13797,  price = 220_000_000,  lifespan = 30 * 52, constructionTime = 42,  Manufacturer("Airbus",  countryCode = "NL"),              runwayRequirement = 3140, imageUrl = "https://www.norebbo.com/2016/11/airbus-a340-600-blank-illustration-templates/"),
    Model("Airbus A340-500",              "Airbus A340",              capacity = 420,   fuelBurn = (8559 / 60).toInt,   speed = 871,  range = 16942,  price = 225_000_000,  lifespan = 30 * 52, constructionTime = 42,  Manufacturer("Airbus",  countryCode = "NL"),              runwayRequirement = 3350, imageUrl = "https://www.norebbo.com/2016/08/airbus-a340-500-blank-illustration-templates/"),
    Model("Airbus A340-300",              "Airbus A340",              capacity = 336,   fuelBurn = (6832 / 60).toInt,   speed = 871,  range = 12259,  price = 190_000_000,  lifespan = 30 * 52, constructionTime = 42,  Manufacturer("Airbus",  countryCode = "NL"),              runwayRequirement = 2743, imageUrl = "https://www.norebbo.com/2016/04/airbus-340-300-and-a340-300x-blank-illustration-templates/"),
    Model("Airbus A340-200",              "Airbus A340",              capacity = 303,   fuelBurn = (6691 / 60).toInt,   speed = 871,  range = 14026,  price = 180_000_000,  lifespan = 30 * 52, constructionTime = 42,  Manufacturer("Airbus",  countryCode = "NL"),              runwayRequirement = 2743, imageUrl = "https://www.norebbo.com/2019/01/airbus-a340-200-side-view/"),
    Model("Airbus A330-900neo",           "Airbus A330",              capacity = 420,   fuelBurn = (5321 / 60).toInt,   speed = 860,  range = 11589,  price = 320_000_000,  lifespan = 35 * 52, constructionTime = 42,  Manufacturer("Airbus",  countryCode = "NL"),              runwayRequirement = 3048, imageUrl = "https://www.norebbo.com/2018/06/airbus-a330-900-neo-blank-illustration-templates/"),
    Model("Airbus A330-800neo",           "Airbus A330",              capacity = 390,   fuelBurn = (5200 / 60).toInt,   speed = 860,  range = 13591,  price = 290_000_000,  lifespan = 35 * 52, constructionTime = 42,  Manufacturer("Airbus",  countryCode = "NL"),              runwayRequirement = 3151, imageUrl = "https://www.norebbo.com/2018/06/airbus-a330-800-neo-blank-illustration-templates/"),
    Model("Airbus A330-300",              "Airbus A330",              capacity = 420,   fuelBurn = (6317 / 60).toInt,   speed = 860,  range = 10510,  price = 270_000_000,  lifespan = 30 * 52, constructionTime = 42,  Manufacturer("Airbus",  countryCode = "NL"),              runwayRequirement = 2209, imageUrl = "https://www.norebbo.com/2016/02/airbus-a330-300-blank-illustration-templates-with-all-three-engine-options/"),
    Model("Airbus A330-200",              "Airbus A330",              capacity = 406,   fuelBurn = (7059 / 60).toInt,   speed = 860,  range = 13092,  price = 240_000_000,  lifespan = 30 * 52, constructionTime = 36,  Manufacturer("Airbus",  countryCode = "NL"),              runwayRequirement = 2445, imageUrl = "https://www.norebbo.com/2016/02/airbus-a330-200-blank-illustration-templates-with-pratt-whitney-engines/"),
    Model("Airbus A321neoXLR",            "Airbus A320",              capacity = 236,   fuelBurn = (3240 / 60).toInt,   speed = 828,  range = 8700,   price = 145_000_000,  lifespan = 35 * 52, constructionTime = 24,  Manufacturer("Airbus",  countryCode = "NL"),              runwayRequirement = 2450, imageUrl = "https://www.norebbo.com/2018/10/airbus-a321neo-lr-long-range-blank-illustration-templates/"),
    Model("Airbus A321neoLR",             "Airbus A320",              capacity = 240,   fuelBurn = (2844 / 60).toInt,   speed = 828,  range = 7400,   price = 140_000_000,  lifespan = 35 * 52, constructionTime = 24,  Manufacturer("Airbus",  countryCode = "NL"),              runwayRequirement = 2315, imageUrl = "https://www.norebbo.com/2018/10/airbus-a321neo-lr-long-range-blank-illustration-templates/"),
    Model("Airbus A321neo",               "Airbus A320",              capacity = 244,   fuelBurn = (2748 / 60).toInt,   speed = 828,  range = 6850,   price = 130_000_000,  lifespan = 35 * 52, constructionTime = 18,  Manufacturer("Airbus",  countryCode = "NL"),              runwayRequirement = 2165, imageUrl = "https://www.norebbo.com/2017/09/airbus-a321-neo-blank-illustration-templates/"),
    Model("Airbus A321",                  "Airbus A320",              capacity = 236,   fuelBurn = (3245 / 60).toInt,   speed = 828,  range = 5930,   price = 100_000_000,  lifespan = 30 * 52, constructionTime = 18,  Manufacturer("Airbus",  countryCode = "NL"),              runwayRequirement = 2210, imageUrl = "https://www.norebbo.com/2014/03/airbus-a321-blank-illustration-templates/"),
    Model("Airbus A320neo",               "Airbus A320",              capacity = 195,   fuelBurn = (2242 / 60).toInt,   speed = 828,  range = 6500,   price = 110_000_000,  lifespan = 35 * 52, constructionTime = 16,  Manufacturer("Airbus",  countryCode = "NL"),              runwayRequirement = 2100, imageUrl = "https://www.norebbo.com/2017/08/airbus-a320-neo-blank-illustration-templates/"),
    Model("Airbus A320",                  "Airbus A320",              capacity = 180,   fuelBurn = (2457 / 60).toInt,   speed = 828,  range = 6150,   price = 95_000_000,   lifespan = 30 * 52, constructionTime = 16,  Manufacturer("Airbus",  countryCode = "NL"),              runwayRequirement = 2100, imageUrl = "https://www.norebbo.com/2013/08/airbus-a320-blank-illustration-templates/"),
    Model("Airbus A319neo",               "Airbus A320",              capacity = 160,   fuelBurn = (2016 / 60).toInt,   speed = 828,  range = 6850,   price = 100_000_000,  lifespan = 35 * 52, constructionTime = 16,  Manufacturer("Airbus",  countryCode = "NL"),             runwayRequirement = 1850, imageUrl = "https://www.norebbo.com/2017/09/airbus-a319-neo-blank-illustration-templates/"),
    Model("Airbus A319",                  "Airbus A320",              capacity = 156,   fuelBurn = (2547 / 60).toInt,   speed = 828,  range = 6940,   price = 77_500_000,   lifespan = 30 * 52, constructionTime = 12,  Manufacturer("Airbus",  countryCode = "NL"),             runwayRequirement = 1850, imageUrl = "https://www.norebbo.com/2014/05/airbus-a319-blank-illustration-templates/"),
    Model("Airbus A318",                  "Airbus A320",              capacity = 132,   fuelBurn = (2098 / 60).toInt,   speed = 828,  range = 7800,   price = 65_000_000,   lifespan = 30 * 52, constructionTime = 12,  Manufacturer("Airbus",  countryCode = "NL"),             runwayRequirement = 1780, imageUrl = "https://www.norebbo.com/airbus-a318-blank-illustration-templates-with-pratt-whitney-and-cfm56-engines/"),
    Model("Airbus A310-300",              "Airbus A300/A310",         capacity = 264,   fuelBurn = (5241 / 60).toInt,   speed = 835,  range = 8364,   price = 135_000_000,  lifespan = 25 * 52, constructionTime = 28,  Manufacturer("Airbus",  countryCode = "NL"),             runwayRequirement = 2285, imageUrl = "https://www.norebbo.com/2015/07/airbus-a310-300-blank-illustration-templates/"),
    Model("Airbus A310-200",              "Airbus A300/A310",         capacity = 264,   fuelBurn = (4840 / 60).toInt,   speed = 835,  range = 6906,   price = 125_000_000,  lifespan = 25 * 52, constructionTime = 28,  Manufacturer("Airbus",  countryCode = "NL"),             runwayRequirement = 2049, imageUrl = "https://www.norebbo.com/2015/07/airbus-a310-300-blank-illustration-templates/"),
    Model("Airbus A300-600",              "Airbus A300/A310",         capacity = 345,   fuelBurn = (6634 / 60).toInt,   speed = 835,  range = 6288,   price = 170_000_000,  lifespan = 25 * 52, constructionTime = 28,  Manufacturer("Airbus",  countryCode = "NL"),             runwayRequirement = 2194, imageUrl = "https://www.norebbo.com/2018/11/airbus-a300b4-600r-blank-illustration-templates-with-general-electric-engines/"))
val modelByName = models.map { model => (model.name, model) }.toMap
}
