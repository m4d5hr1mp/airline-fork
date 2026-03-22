package com.patson

import java.util.{ArrayList, Collections}
import com.patson.data.{AirportSource, CountrySource, EventSource}
import com.patson.util.RelationshipCache
import com.patson.model.event.{EventType, Olympics}
import com.patson.model.{PassengerType, _}
// Import for demand decompression over time!
import com.patson.ChronologyConverter

import java.util.concurrent.ThreadLocalRandom
import scala.collection.immutable.Map
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.collection.parallel.CollectionConverters._
import scala.util.Random
import FlightType._


object DemandGenerator {
  def main(args : Array[String]) : Unit = {
    println("Loading airports")
    val airports = AirportSource.loadAllAirports(true, true)
    val airportsByIata = airports.map(a => (a.iata, a)).toMap
    println(s"Loaded ${airports.length} airports")
    val demand = computeDemand(0, airports)
    println(s"Demand chunks ${demand.length}. Demand total pax ${demand.map(_._3).sum}")


    val pairs : List[(String, String)] = List(
      // USA - Major metro pairs (cross country)
      "JFK" -> "LAX", "ORD" -> "DFW", "SFO" -> "MIA", "ATL" -> "SEA", "BOS" -> "DEN",

      // USA - Major + medium metro 500-1500km
      "JFK" -> "PIT", "LAX" -> "PHX", "ORD" -> "MSP", "ATL" -> "MEM", "DFW" -> "STL",

      // USA - Major + small/semi-rural under 700km
      "JFK" -> "ALB", "ORD" -> "GRR", "LAX" -> "FAT", "ATL" -> "HSV", "DFW" -> "LBB",

      // USA - Major intl North America
      "JFK" -> "YYZ", "LAX" -> "YVR", "MIA" -> "MEX", "ORD" -> "YUL", "DFW" -> "CUN",

      // USA - Major European destinations
      "JFK" -> "LHR", "JFK" -> "CDG", "JFK" -> "FRA", "ORD" -> "LHR", "MIA" -> "MAD",

      // USA - Secondary European destinations
      "JFK" -> "DUB", "BOS" -> "LIS", "JFK" -> "ZRH", "ATL" -> "FCO", "JFK" -> "AMS",

      // USA - Major Asia destinations
      "LAX" -> "NRT", "LAX" -> "ICN", "LAX" -> "HKG", "SFO" -> "PEK", "LAX" -> "SIN",

      // USA - Oceania
      "LAX" -> "SYD", "LAX" -> "AKL", "SFO" -> "MEL",

      // Europe - Major pairs (DE/FR/GB/IT/NL)
      "LHR" -> "CDG", "LHR" -> "FRA", "CDG" -> "FRA", "AMS" -> "LHR", "FCO" -> "CDG",

      // Europe - Smaller pairs
      "EDI" -> "AMS", "BHX" -> "CDG", "MAN" -> "FCO", "LYS" -> "FRA", "CPH" -> "AMS",
      "DUS" -> "LHR", "HAM" -> "CDG", "NCE" -> "LHR", "BCN" -> "LHR", "MUC" -> "FCO",

      // Europe - Large to small
      "LHR" -> "EXE", "CDG" -> "BES", "FRA" -> "ERF", "AMS" -> "GRQ", "FCO" -> "BRI",
      "LHR" -> "INV", "CDG" -> "MPL", "MUC" -> "NUE", "MAD" -> "VGO", "BCN" -> "PMI",

      // Europe - Major EU to Eastern block capitals
      "LHR" -> "WAW", "CDG" -> "BUD", "FRA" -> "PRG", "AMS" -> "BEG", "LHR" -> "SOF",

      // Africa - Major pairs
      "JNB" -> "NBO", "CAI" -> "LOS", "JNB" -> "CPT", "ADD" -> "NBO", "LOS" -> "ACC",

      // LatAm - Major pairs
      "GRU" -> "EZE", "GRU" -> "BOG", "MEX" -> "BOG", "LIM" -> "BOG", "GRU" -> "SCL",

      // LatAm - Major to medium
      "GRU" -> "CWB", "GRU" -> "BSB", "MEX" -> "GDL", "MEX" -> "MTY", "BOG" -> "MDE",
      "SCL" -> "IQQ", "EZE" -> "COR", "LIM" -> "CUZ", "GRU" -> "FOR", "MEX" -> "CUN",

      // Asia Pacific - Major pairs
      "SYD" -> "MEL", "SYD" -> "SIN", "NRT" -> "SYD", "ICN" -> "SYD", "SIN" -> "SYD",

      // Asia Pacific - Major to medium
      "SYD" -> "BNE", "SYD" -> "PER", "MEL" -> "ADL", "SYD" -> "CNS", "NRT" -> "MEL",
      "SIN" -> "MEL", "AKL" -> "SYD", "BNE" -> "AKL", "MEL" -> "CHC", "SYD" -> "DPS",

      // Asia Pacific - Medium to small
      "BNE" -> "CNS", "MEL" -> "HBA", "AKL" -> "WLG", "CHC" -> "WLG", "SYD" -> "OOL",
      "BNE" -> "TSV", "MEL" -> "MKY", "AKL" -> "ZQN", "PER" -> "BNE", "HBA" -> "ADL",

      // Asia SEA - Major pairs
      "SIN" -> "BKK", "SIN" -> "KUL", "BKK" -> "CGK", "SIN" -> "CGK", "BKK" -> "HAN",

      // Asia SEA - Major to medium <1000km
      "SIN" -> "PEN", "BKK" -> "RGN", "CGK" -> "DPS", "MNL" -> "CEB", "HAN" -> "SGN",
      "BKK" -> "CNX", "SIN" -> "SUB", "KUL" -> "BKI", "CGK" -> "UPG", "MNL" -> "MNL",

      // Asia Southern - Major pairs
      "DEL" -> "BOM", "DEL" -> "MAA", "BOM" -> "BLR", "DEL" -> "CCU", "BOM" -> "HYD",

      // Asia Southern - Major to medium
      "DEL" -> "JAI", "DEL" -> "AMD", "BOM" -> "GOI", "DEL" -> "LKO", "MAA" -> "BLR",
      "DEL" -> "ATQ", "BOM" -> "NAG", "DEL" -> "PAT", "CCU" -> "IXB", "BLR" -> "HYD",

      // MENA - Major pairs
      "DXB" -> "CAI", "DXB" -> "IST", "RUH" -> "DXB", "IST" -> "CAI", "DXB" -> "AMM",

      // MENA - Major to medium
      "DXB" -> "MCT", "RUH" -> "JED", "CAI" -> "HRG", "IST" -> "AYT", "DXB" -> "KWI",
      "AMM" -> "BEY", "CAI" -> "SSH", "IST" -> "ESB", "DXB" -> "BAH", "RUH" -> "KWI",

      // China - Major pairs
      "PEK" -> "SHA", "PEK" -> "CAN", "SHA" -> "CAN", "PEK" -> "CTU", "PVG" -> "SZX",

      // China - Major to medium
      "PEK" -> "WUH", "PVG" -> "XIY", "CAN" -> "CKG", "PEK" -> "DLC", "PVG" -> "HGH",
      "CTU" -> "KMG", "PEK" -> "NKG", "CAN" -> "CSX", "PVG" -> "TAO", "PEK" -> "TSN",

      // China - Medium to small
      "WUH" -> "CKG", "XIY" -> "CTU", "KMG" -> "CTU", "NKG" -> "WUH", "DLC" -> "SHE",
      "HRB" -> "SHE", "CSX" -> "WUH", "KWE" -> "CTU", "NNG" -> "CAN", "TAO" -> "NKG",

      // Eastern Block - Major pairs
      "SVO" -> "LED", "WAW" -> "BUD", "PRG" -> "WAW", "BUD" -> "BEG", "SVO" -> "KBP",

      // Eastern Block - Major to medium <1000km
      "SVO" -> "KZN", "WAW" -> "KRK", "PRG" -> "BRQ", "BUD" -> "DEB", "SVO" -> "ROV",
      "WAW" -> "GDN", "BEG" -> "SKP", "SVO" -> "UFA", "LED" -> "KZN", "PRG" -> "OLO",

      // Eastern Block - Medium to small
      "KRK" -> "WRO", "KZN" -> "UFA", "SKP" -> "TIA", "BEG" -> "OHD", "WRO" -> "GDN",
      "UFA" -> "KZN", "SVX" -> "KZN", "ROV" -> "KZN", "DEB" -> "MIS", "BRQ" -> "OSR",
    )

    pairs.foreach {
      case (a1, a2) =>
        var y = 0
        var j = 0
        var f = 0
        demand.foreach {
          case(group, toAirport, pax) =>
            if ((group.fromAirport.iata == a1 && toAirport.iata == a2) || (group.fromAirport.iata == a2 && toAirport.iata == a1)) {
              if (group.preference.preferredLinkClass == FIRST) {
                f += pax
              } else if (group.preference.preferredLinkClass == BUSINESS) {
                j += pax
              } else {
                y += pax
              }
            }
        }
        println(s"$a1 -> $a2 $y/$j/$f")
    }
  }
  private[this] val FIRST_CLASS_INCOME_MAX = 100_000
  private[this] val FIRST_CLASS_PERCENTAGE_MAX = Map(PassengerType.BUSINESS -> 0.08, PassengerType.TOURIST -> 0.02, PassengerType.OLYMPICS -> 0.03) //max 8% first (Business passenger), 2% first (Tourist)
  private[this] val BUSINESS_CLASS_INCOME_MAX = 100_000
  private[this] val BUSINESS_CLASS_PERCENTAGE_MAX = Map(PassengerType.BUSINESS -> 0.3, PassengerType.TOURIST -> 0.10, PassengerType.OLYMPICS -> 0.15) //max 30% business (Business passenger), 10% business (Tourist)
  // For demand nerfing lol.
  private val COMPRESSION_EXP_START = 0.725
  private val COMPRESSION_EXP_END   = 0.725
  private val COMPRESSION_END_CYCLE = ChronologyConverter.cyclesPerYear * 20

private def demandCompressionExponent(cycle: Int): Double = {
  val t = math.min(1.0, cycle.toDouble / COMPRESSION_END_CYCLE)
  COMPRESSION_EXP_START + (COMPRESSION_EXP_END - COMPRESSION_EXP_START) * t
}


  private val DROP_DEMAND_THRESHOLDS = new Array[Int](FlightType.values.size)

  val MIN_DISTANCE = 50
  val DIMINISHED_DEMAND_THRESHOLD = 400 //distance within this range will be diminished
  
  import scala.collection.JavaConverters._

  def computeDemand(cycle: Int, sourceAirports : List[Airport], plainDemand : Boolean = false) : List[(PassengerGroup, Airport, Int)] = {
    val airports =
      if (plainDemand) {
        sourceAirports.map(airport => {
          val clone = airport.copy()
          clone.initAssets(List.empty)
          clone.initFeatures(airport.features.filter(_.featureType != AirportFeatureType.AVIATION_HUB))
          clone.initAirlineBases(List.empty)
          clone
        })
      } else {
        sourceAirports
      }

    val allDemands = new ArrayList[(Airport, List[(Airport, (PassengerType.Value, LinkClassValues))])]()
	  
	  airports.foreach {  fromAirport =>
	    val demandList = Collections.synchronizedList(new ArrayList[(Airport, (PassengerType.Value, LinkClassValues))]())
	    airports.par.foreach { toAirport =>
        val relationship = RelationshipCache.getRelationship(fromAirport.countryCode, toAirport.countryCode)
        val businessDemand = computeDemandBetweenAirports(fromAirport, toAirport, relationship, PassengerType.BUSINESS)
        val touristDemand = computeDemandBetweenAirports(fromAirport, toAirport, relationship, PassengerType.TOURIST)
    	          
        if (businessDemand.total > 0) {
          demandList.add((toAirport, (PassengerType.BUSINESS, businessDemand)))
        } 
        if (touristDemand.total > 0) {
          demandList.add((toAirport, (PassengerType.TOURIST, touristDemand)))
        }
	    }
	    allDemands.add((fromAirport, demandList.asScala.toList))
    }

    val allDemandsAsScala = allDemands.asScala

    val exp = demandCompressionExponent(cycle)
      if (exp < 1.0) {
        val pairTotals = mutable.HashMap[(Int, Int), Int]()
        allDemandsAsScala.foreach { case (from, toList) =>
          toList.foreach { case (to, (_, demand)) =>
            val key = if (from.id < to.id) (from.id, to.id) else (to.id, from.id)
            pairTotals(key) = pairTotals.getOrElse(key, 0) + demand.total
          }
        }
        val pairScales = pairTotals.map { case (key, total) =>
          val scale = if (total <= 100) 1.0 else math.pow(100.0 / total, 1.0 - exp)
          (key, scale)
        }.toMap
        val compressed = allDemandsAsScala.map { case (from, toList) =>
          val newToList = toList.flatMap { case (to, (pType, demand)) =>
            val key = if (from.id < to.id) (from.id, to.id) else (to.id, from.id)
            val scaled = demand * pairScales(key)
            if (scaled.total > 0) Some((to, (pType, scaled))) else None
          }
          (from, newToList)
        }
        allDemandsAsScala.clear()
        allDemandsAsScala.appendAll(compressed)
      }

    if (!plainDemand) {
      allDemandsAsScala.appendAll(generateEventDemand(cycle, airports))
    }

	  val baseDemandChunkSize = 10
	  
	  val allDemandChunks = ListBuffer[(PassengerGroup, Airport, Int)]()
    var oneCount = 0
	  allDemandsAsScala.foreach {
	    case (fromAirport, toAirportsWithDemand) =>
        //for each city generate different preferences
        val flightPreferencesPool = getFlightPreferencePoolOnAirport(fromAirport)

        toAirportsWithDemand.foreach {
          case (toAirport, (passengerType, demand)) =>
            LinkClass.values.foreach { linkClass =>
              if (demand(linkClass) > 0) {
                var remainingDemand = demand(linkClass)
                var demandChunkSize = baseDemandChunkSize + ThreadLocalRandom.current().nextInt(baseDemandChunkSize)
                while (remainingDemand > demandChunkSize) {
                  allDemandChunks.append((PassengerGroup(fromAirport, flightPreferencesPool.draw(linkClass, fromAirport, toAirport), passengerType), toAirport, demandChunkSize))
                  remainingDemand -= demandChunkSize
                  demandChunkSize = baseDemandChunkSize + ThreadLocalRandom.current().nextInt(baseDemandChunkSize)
                }
                allDemandChunks.append((PassengerGroup(fromAirport, flightPreferencesPool.draw(linkClass, fromAirport, toAirport), passengerType), toAirport, remainingDemand)) // don't forget the last chunk
              }
            }
        }
	  }
    allDemandChunks.toList
  }

  def computeDemandBetweenAirports(fromAirport : Airport, toAirport : Airport, relationship : Int, passengerType : PassengerType.Value) : LinkClassValues = {
    val distance = Computation.calculateDistance(fromAirport, toAirport)
    if (fromAirport == toAirport || fromAirport.population == 0 || toAirport.population == 0 || distance <= MIN_DISTANCE) {
      LinkClassValues.getInstance(0, 0, 0)
    } else {
      val flightType = Computation.getFlightType(fromAirport, toAirport, distance)

      //assumption - 1 passenger each week from airport with 1 million pop and 50k income will want to travel to an airport with 1 million pop at income level 25 for business
      //             0.3 passenger in same condition for sightseeing (very low as it should be mainly driven by feature)
      //we are using income level for to airport as destination income difference should have less impact on demand compared to origination airport (and income level is log(income))
      val toAirportIncomeLevel = toAirport.incomeLevel

      val lowIncomeThreshold = Country.LOW_INCOME_THRESHOLD + 10_000 //due to a bug in v2, we need to increase this a bit to avoid demand collapse in low income countries

      val fromAirportAdjustedIncome : Double = if (fromAirport.income > Country.HIGH_INCOME_THRESHOLD) { //to make high income airport a little bit less overpowered
        Country.HIGH_INCOME_THRESHOLD + (fromAirport.income - Country.HIGH_INCOME_THRESHOLD) / 3
      } else if (fromAirport.income < lowIncomeThreshold) { //to make low income airport a bit stronger
        val delta = lowIncomeThreshold - fromAirport.income
        lowIncomeThreshold - delta * 0.3 //so a 0 income country will be boosted to 21000, a 10000 income country will be boosted to 24000
      } else {
        fromAirport.income
      }
        
      val fromAirportAdjustedPower = fromAirportAdjustedIncome * fromAirport.population

      val ADJUST_FACTOR = 0.35

      var baseDemand: Double = (fromAirportAdjustedPower.doubleValue() / 1000000 / 50000) * (toAirport.population.doubleValue() / 1000000 * toAirportIncomeLevel / 10) * (passengerType match {
        case PassengerType.BUSINESS => 6
        case PassengerType.TOURIST | PassengerType.OLYMPICS =>
          if (fromAirport.incomeLevel > 25) {
            1 + (fromAirport.incomeLevel - 25) / 10
          } else {
            1
          }
      }) * ADJUST_FACTOR

      if (!RelationshipCache.isSameMarket(fromAirport.countryCode, toAirport.countryCode)) {
        //baseDemand = baseDemand *
        val mutliplier = 
            if (relationship <= -3) 0 
            else if (relationship == -2) 0.1
            else if (relationship == -1) 0.2
            else if (relationship == 0) 0.5
            else if (relationship == 1) 0.8
            else if (relationship == 2) 1
            else if (relationship == 3) 1.5
            else 2 // >= 4
        baseDemand = baseDemand * mutliplier
      }

      var adjustedDemand = baseDemand
      //adjustment : extra bonus to tourist supply for rich airports, up to double at every 20 income level increment

      //bonus for domestic and short-haul flight
      adjustedDemand += baseDemand * (flightType match {
        case SHORT_HAUL_DOMESTIC => 4.0 //people would just drive or take other transit
        case MEDIUM_HAUL_DOMESTIC | LONG_HAUL_DOMESTIC => 7.0
        case SHORT_HAUL_INTERNATIONAL | MEDIUM_HAUL_INTERNATIONAL | SHORT_HAUL_INTERCONTINENTAL | MEDIUM_HAUL_INTERCONTINENTAL => 0
        case LONG_HAUL_INTERNATIONAL | LONG_HAUL_INTERCONTINENTAL => -0.5
        case ULTRA_LONG_HAUL_INTERCONTINENTAL => -0.75
      })
      
      
      //adjustments : these zones do not have good ground transport
      if (fromAirport.zone == toAirport.zone) {

        //For Africa adjusted demand is 1 + 2
        if (fromAirport.zone == "AF") {
          adjustedDemand +=  baseDemand * 2

        // For LatAm adjusted demand is 1 + 1
        } else if (fromAirport.zone == "SA") {
          adjustedDemand +=  baseDemand * 1

        // For Oceania adjusted demand is 1 + 1
        } else if (fromAirport.zone == "OC") {
          adjustedDemand +=  baseDemand * 1
        }
      }

      //adjustments : China has very extensive highspeed rail network
      if (fromAirport.countryCode == "CN" && toAirport.countryCode == "CN") {
        adjustedDemand *= 0.5
      }

      //adjust by features
      fromAirport.getFeatures().foreach { feature =>
        val adjustment = feature.demandAdjustment(baseDemand, passengerType, fromAirport.id, fromAirport, toAirport, flightType, relationship)
        adjustedDemand += adjustment
      }
      toAirport.getFeatures().foreach { feature => 
        val adjustment = feature.demandAdjustment(baseDemand, passengerType, toAirport.id, fromAirport, toAirport, flightType, relationship)
        adjustedDemand += adjustment
      }

      if (adjustedDemand >= 100 && distance < DIMINISHED_DEMAND_THRESHOLD) { //diminished demand for ultra short routes
        adjustedDemand = 100 + (adjustedDemand - 100) * (distance - MIN_DISTANCE) / (DIMINISHED_DEMAND_THRESHOLD - MIN_DISTANCE)
      }
      
      //compute demand composition. depends on from airport income
      val income = fromAirport.income

      var firstClassPercentage : Double =
        if (flightType != SHORT_HAUL_DOMESTIC) {
          if (income >= FIRST_CLASS_INCOME_MAX) {
            FIRST_CLASS_PERCENTAGE_MAX(passengerType) 
          } else { 
            FIRST_CLASS_PERCENTAGE_MAX(passengerType) * income / FIRST_CLASS_INCOME_MAX
          }
        } else {
         0 
        }
      var businessClassPercentage : Double = {
        if (income >= BUSINESS_CLASS_INCOME_MAX) {
          BUSINESS_CLASS_PERCENTAGE_MAX(passengerType)
        } else {
          BUSINESS_CLASS_PERCENTAGE_MAX(passengerType) * income / BUSINESS_CLASS_INCOME_MAX
        }
      }

      if (flightType == SHORT_HAUL_DOMESTIC) {
        firstClassPercentage *= 0.5
        businessClassPercentage *= 0.5
      } else if (flightType == SHORT_HAUL_INTERNATIONAL || flightType == SHORT_HAUL_INTERCONTINENTAL) {
        firstClassPercentage *= 0.75
        businessClassPercentage *= 0.75
      }

      var firstClassDemand = (adjustedDemand * firstClassPercentage).toInt
      var businessClassDemand = (adjustedDemand * businessClassPercentage).toInt
      val economyClassDemand = adjustedDemand.toInt - firstClassDemand - businessClassDemand
      
      //add extra business and first class demand from lounge for major airports
      if (fromAirport.size >= Lounge.LOUNGE_PASSENGER_AIRPORT_SIZE_REQUIREMENT && toAirport.size >= Lounge.LOUNGE_PASSENGER_AIRPORT_SIZE_REQUIREMENT) { 
        firstClassDemand = (firstClassDemand * 2.5).toInt
        businessClassDemand = (businessClassDemand * 2.5).toInt
      }

      val demand = LinkClassValues.getInstance(economyClassDemand, businessClassDemand, firstClassDemand)

      demand
    }
  }


  def generateEventDemand(cycle : Int, airports : List[Airport]) : List[(Airport, List[(Airport, (PassengerType.Value, LinkClassValues))])] = {
    val eventDemand = ListBuffer[(Airport, List[(Airport, (PassengerType.Value, LinkClassValues))])]()
    EventSource.loadEvents().filter(_.isActive(cycle)).foreach { event =>
      event match {
        case olympics : Olympics => eventDemand.appendAll(generateOlympicsDemand(cycle, olympics, airports))
        case _ => //
      }
    }
    eventDemand.toList
  }

  val OLYMPICS_DEMAND_BASE = 50000
  def generateOlympicsDemand(cycle: Int, olympics : Olympics, airports : List[Airport]) : List[(Airport, List[(Airport, (PassengerType.Value, LinkClassValues))])]  = {
    if (olympics.currentYear(cycle) == 4) { //only has special demand on 4th year
      val week = (cycle - olympics.startCycle) % Olympics.WEEKS_PER_YEAR //which week is this
      val demandMultiplier = Olympics.getDemandMultiplier(week)
      Olympics.getSelectedAirport(olympics.id) match {
        case Some(selectedAirport) => generateOlympicsDemand(cycle, demandMultiplier, Olympics.getAffectedAirport(olympics.id, selectedAirport), airports)
        case None => List.empty
      }
    } else {
      List.empty
    }

  }

  def generateOlympicsDemand(cycle: Int, demandMultiplier : Int, olympicsAirports : List[Airport], allAirports : List[Airport]) : List[(Airport, List[(Airport, (PassengerType.Value, LinkClassValues))])]  = {
    val totalDemand = OLYMPICS_DEMAND_BASE * demandMultiplier

    //use existing logic, just scale the total back to totalDemand at the end
    val unscaledDemands = ListBuffer[(Airport, List[(Airport, (PassengerType.Value, LinkClassValues))])]()
    val otherAirports = allAirports.filter(airport => !olympicsAirports.map(_.id).contains(airport.id))

    otherAirports.foreach { airport =>
      val unscaledDemandsOfThisFromAirport = ListBuffer[(Airport, (PassengerType.Value, LinkClassValues))]()
      val fromAirport = airport
      olympicsAirports.foreach {  olympicsAirport =>
        val toAirport = olympicsAirport
        val relationship = RelationshipCache.getRelationship(fromAirport.countryCode, toAirport.countryCode)
        val computedDemand = computeDemandBetweenAirports(fromAirport, toAirport, relationship, PassengerType.OLYMPICS)
        if (computedDemand.total > 0) {
          unscaledDemandsOfThisFromAirport.append((toAirport, (PassengerType.OLYMPICS, computedDemand)))
        }
      }
      unscaledDemands.append((fromAirport, unscaledDemandsOfThisFromAirport.toList))
    }

    //now scale all the demands based on the totalDemand
    val unscaledTotalDemands = unscaledDemands.map {
      case (toAirport, unscaledDemandsOfThisToAirport) => unscaledDemandsOfThisToAirport.map {
        case (fromAirport, (passengerType, demand)) => demand.total
      }.sum
    }.sum
    val multiplier = totalDemand.toDouble / unscaledTotalDemands
    println(s"olympics scale multiplier is $multiplier")
    val scaledDemands = unscaledDemands.map {
      case (toAirport, unscaledDemandsOfThisToAirport) =>
        (toAirport, unscaledDemandsOfThisToAirport.map {
          case (fromAirport, (passengerType, unscaledDemand)) =>
            (fromAirport, (passengerType, unscaledDemand * multiplier))
        })
    }.toList
    scaledDemands
  }
  
  def getFlightPreferencePoolOnAirport(homeAirport : Airport) : FlightPreferencePool = {
    val flightPreferences = ListBuffer[(FlightPreference, Int)]()
    val budgetTravelerMultiplier =
      if (homeAirport.income < Country.LOW_INCOME_THRESHOLD / 2) {
        3
      } else if (homeAirport.income < Country.LOW_INCOME_THRESHOLD) {
    	  2
  	  } else {
        1
      }
    
    for (i <- 0 until budgetTravelerMultiplier) {
      flightPreferences.append((SimplePreference(homeAirport, 1.2, ECONOMY), 2))
      flightPreferences.append((SimplePreference(homeAirport, 1.3, ECONOMY), 2)) //quite sensitive to price
      flightPreferences.append((SimplePreference(homeAirport, 1.4, ECONOMY), 1)) //very sensitive to price
      flightPreferences.append((SimplePreference(homeAirport, 1.5, ECONOMY), 1)) //very sensitive to price
    }
    
    flightPreferences.append((SpeedPreference(homeAirport, ECONOMY), 2))
    flightPreferences.append((AppealPreference.getAppealPreferenceWithId(homeAirport, ECONOMY, loungeLevelRequired = 0), 4))
    flightPreferences.append((AppealPreference.getAppealPreferenceWithId(homeAirport, ECONOMY, loungeLevelRequired = 0), 4))
    flightPreferences.append((AppealPreference.getAppealPreferenceWithId(homeAirport, ECONOMY, loungeLevelRequired = 0, loyaltyRatio = 1.1), 2))
    flightPreferences.append((AppealPreference.getAppealPreferenceWithId(homeAirport, ECONOMY, loungeLevelRequired = 0, loyaltyRatio = 1.2), 1))
    
    
    //BUSINESS prefs
    for (i <- 0 until 2) { //bit more randomness - set variation per group
      flightPreferences.append((SpeedPreference(homeAirport, BUSINESS), 3))
      flightPreferences.append((AppealPreference.getAppealPreferenceWithId(homeAirport, BUSINESS, loungeLevelRequired = 0), 2))
      flightPreferences.append((AppealPreference.getAppealPreferenceWithId(homeAirport, BUSINESS, loungeLevelRequired = 0, loyaltyRatio = 1), 2))
      flightPreferences.append((AppealPreference.getAppealPreferenceWithId(homeAirport, BUSINESS, loungeLevelRequired = 1, loyaltyRatio = 1.1), 1))
      flightPreferences.append((AppealPreference.getAppealPreferenceWithId(homeAirport, BUSINESS, loungeLevelRequired = 2, loyaltyRatio = 1.1), 1))
      flightPreferences.append((AppealPreference.getAppealPreferenceWithId(homeAirport, BUSINESS, loungeLevelRequired = 3, loyaltyRatio = 1.2), 1))
    }
    
    //FIRST prefs 
    flightPreferences.append((SpeedPreference(homeAirport, FIRST), 1))
    flightPreferences.append((AppealPreference.getAppealPreferenceWithId(homeAirport, FIRST, loungeLevelRequired = 0, loyaltyRatio = 1), 2))
    flightPreferences.append((AppealPreference.getAppealPreferenceWithId(homeAirport, FIRST, loungeLevelRequired = 1, loyaltyRatio = 1.1), 1))
    flightPreferences.append((AppealPreference.getAppealPreferenceWithId(homeAirport, FIRST, loungeLevelRequired = 2, loyaltyRatio = 1.1), 1))
    flightPreferences.append((AppealPreference.getAppealPreferenceWithId(homeAirport, FIRST, loungeLevelRequired = 3, loyaltyRatio = 1.2), 1))
    
    new FlightPreferencePool(flightPreferences.toList)
  }
  
  sealed case class Demand(businessDemand : LinkClassValues, touristDemand : LinkClassValues)
}