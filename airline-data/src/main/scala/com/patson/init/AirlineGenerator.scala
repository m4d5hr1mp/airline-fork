package com.patson.init
import scala.collection.mutable.Set
import scala.collection.mutable.ListBuffer
import com.patson.data._
import com.patson.data.Constants._
import com.patson.model._
import com.patson.model.airplane._
import java.util.Calendar
import com.patson.Authentication
import scala.util.Random
import com.patson.DemandGenerator
import com.patson.data._
import com.patson.data.airplane._
import scala.collection.mutable.ArrayBuffer
import com.patson.util.LogoGenerator
import java.awt.Color
import java.util.concurrent.ThreadLocalRandom
import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.Duration
object AirlineGenerator extends App {
  mainFlow
 
  def mainFlow() = {
    generateAirlines()
    println("DONE Creating airlines")
    Await.result(actorSystem.terminate(), Duration.Inf)
  }
 
  def generateAirlines() : Unit = {
    // Explicit list of base IATA codes
    // Add duplicates for multiple bots per airport.
    // Expand with more countries as needed!
    val baseIATAs: List[String] = List(
      // USA - 26 Airports
      "JFK", "BOS", "PHL", "BWI", "IAD",
      "CLT", "ATL", "MIA", "DTW", "BNA",
      "ORD", "MCI", "MSP", "DFW", "IAH",
      "AUS", "DEN", "PHX", "LAS", "SAN",
      "LAX", "ONT", "SFO", "SJC", "PDX",
      "SEA",
      // Canada - 4 Airports
      "YYZ", "YUL", "YVR", "YYC",
      // Mexico - 10 Airports
      "ACA", "PVR", "CUN", "BJX", "QRO",
      "GDL", "MEX", "TLC", "PBC", "MTY",
      // Caribbean - 3 Airports
      "SJU", "KIN", "SJO",
      // LatAm - 4 Airports
      "CCS", "BOG", "LIM", "SCL",
      // Argentine - 4 Airports
      "EZE", "AEP", "MVD", "COR",
      // Brazilia - 8 Airports
      "POA", "GRU", "VCP", "CGH", "GIG",
      "BSB", "SSA", "FOR",
      // Australia & NZ - 6 Airports
      "SYD", "MEL", "ADL", "PER", "BNE",
      "AKL",
      // South-East Asia - 14 Airports
      "CGK", "KNO", "SIN", "KUL", "BKK",
      "DMK", "BWN", "SGN", "VTE", "PKZ",
      "KTI", "HAN", "MNL", "RGN",
      // China, Taiwan, HKG & Macau - 21 Airports
      "HKG", "MFM", "TPE", "CAN", "SZX",
      "KMG", "CTU", "TFU", "WUH", "XMN",
      "PVG", "PVG", "SHA", "CZX", "XIY",
      "TAO", "PEK", "PEK", "PKX", "TSN",
      "HRB",
      // KR and JP - 13 Airports
      "ICN", "GMP", "CJU", "PUS", "FUK",
      "ITM", "KIX", "NGO", "HND", "HND",
      "NRT", "CTS", "CTS",
      // Indo - 21 Airports
      "DAC", "CCU", "KTM", "DEL", "DEL",
      "HDO", "NAG", "HYD", "HYD", "MAA",
      "BLR", "CMB", "COK", "GOI", "BOM",
      "NMI", "AMD", "KHI", "ISB", "ATQ",
      "LHE",
      // Middle East - 9 Airports
      "KBL", "MHD", "THR", "IKA", "BGW",
      "BEY", "DAM", "AMM", "TLV",
      // Gulf states - 6 Airports
      "KWI", "BAH", "DOH", "AUH", "DXB",
      "MCT",
      // Central Asian Republics - 5 airports
      "ASB", "DYU", "TAS", "ALA", "NQZ",
      // CIS - 22 Airports
      "SVO", "DME", "VKO", "LED", "MSQ",
      "KZN", "ROV", "AER", "VOG", "KUF",
      "SVX", "OVB", "IKT", "KHV", "VVO",
      "KBP", "ODS", "SIP", "HRK", "GYD",
      "EVN", "TBS",
      // Baltics & Scandinavia - 8 Airports
      "HEL", "TLL", "RIX", "KUN", "ARN",
      "CPH", "OSL", "KEF",
      // Central Europe - 8 Airports
      "WAW", "KTW", "BTS", "PRG", "BUD",
      "VIE", "ZRH", "GVA",
      // Western Europe + UK - 23 Airlines, 20 Airports (FRA, CDG & LHR have 2x bots)
      "FRA", "FRA", "MUC", "BER", "HAM",
      "CGN", "DUS", "CDG", "CDG", "ORY",
      "LYS", "MRS", "BOD", "BRU", "LUX",
      "AMS", "LHR", "LHR", "LGW", "STN",
      "MAN", "BHX", "DUB",
      // Balkans + Turkey - 17 Airports
      "RMO", "OTP", "SOF", "BOJ", "BEG",
      "SKP", "SJJ", "ZAG", "LJU", "TIA",
      "IST", "SAW", "ADB", "AYT", "ESB",
      "ATH", "SKG",
      // Southern Europe + Islands - 14 Airports
      "MXP", "LIN", "VCE", "FCO", "VRN",
      "NAP", "MLA", "BCN", "MAD", "LIS",
      "ALC", "AGP", "TFS", "LPA",
      // Northern Africa - 5 Airports
      "CMN", "ALG", "TUN", "MJI", "LCA",
      // Sub-Saharan - 2 Airports
      "LOS", "LBV",
      // East Africa - 4 Airprots
      "ADD", "NBO", "DAR", "KGL",
      // Southern Africa - 3 Airports
      "HRE", "JNB", "CPT"
      // Total Bots: 260
    )
    // Loads All airports and sorts them by airport power
    val airports = AirportSource.loadAllAirports(true).sortBy { _.power }
    // Creates a map of airports by IATA
    val airportByIata = airports.map(a => (a.iata, a)).toMap
    // Loads all airplane models, groups them by their family
    val models = ModelSource.loadAllModels()
    val modelsByFamily = models.groupBy(_.family)
    // Creates a map of types allowed for different route types:
    import Model.Type._
    val regionalFamilies = models.filter(_.airplaneType == REGIONAL).map(_.family).distinct
    val narrowbodyFamilies = models.filter(_.airplaneType == MEDIUM).map(_.family).distinct
    val widebodyFamilies = models.filter(m => Set(LARGE, X_LARGE).contains(m.airplaneType)).map(_.family).distinct
    val familiesByCategory = Map(
      "regional" -> regionalFamilies,
      "narrowbody" -> narrowbodyFamilies,
      "widebody" -> widebodyFamilies
    )
    // Get mutual relations between countries, get countries grouped by zones
    val countryRelationships = CountrySource.getCountryMutualRelationships()
    val airportsByZone = airports.groupBy { _.zone }
    for (i <- baseIATAs.indices) {
      val baseIata = baseIATAs(i)
      val baseAirport = airportByIata.getOrElse(baseIata, throw new IllegalArgumentException(s"Airport $baseIata not found"))
      // Calculate instance number for uniqueness (1 for first, 2 for second, etc.)
      val instanceNumber = baseIATAs.take(i + 1).count(_ == baseIata)
      val instanceSuffix = if (instanceNumber > 1) instanceNumber else ""
      val userName = baseAirport.iata + instanceSuffix
      val user = User(userName = userName, email = "", Calendar.getInstance, Calendar.getInstance, UserStatus.ACTIVE, level = 0, None, List.empty)
      UserSource.saveUser(user)
      Authentication.createUserSecret(userName, "1234")
      // Sets Airline Name (Displayed In-Game) as "Air-SVO-Secondary" or "Air-SVO-Primary" for example
      val airlineSuffix = instanceNumber match {
        case 1 => "Primary"
        case 2 => "Secondary"
        case 3 => "Tertiary"
        case _ => "Instance" + instanceNumber
      }
      val airlineName = "Air-" + baseAirport.iata + "-" + airlineSuffix
      val newAirline = Airline(airlineName, isGenerated = true)
      newAirline.setBalance(0)
      newAirline.setMaintenanceQuality(100)
      newAirline.setTargetServiceQuality(30)
      newAirline.setCountryCode(baseAirport.countryCode)
      newAirline.setAirlineCode(newAirline.getDefaultAirlineCode())
      // Create base at scale (level) 10, foundedCycle 1
      val airlineBase = AirlineBase(newAirline, baseAirport, baseAirport.countryCode, 10, 1, true)
      // Saves All Bot Airlines to DB (Airlines only, not bases or routes)
      AirlineSource.saveAirlines(List(newAirline))
      // Generate Random Logo and save to DB as well
      AirlineSource.saveLogo(newAirline.id, LogoGenerator.generateRandomLogo())
      UserSource.setUserAirline(user, newAirline)
      // Saves bases
      AirlineSource.saveAirlineBase(airlineBase)
      // Sets airplane renewal condition
      AirlineSource.saveAirplaneRenewal(newAirline.id, 50)
      println(i + " generated user " + user.userName)
      //====================================================
      //
      // ROUTE GENERATION + PLANE SELECTION BELOW THIS POINT
      //
      //====================================================
      // Bots only fly IC from Scale 8+ Airports and Gateways!
      val permittedIC = baseAirport.isGateway || baseAirport.size >= 8
      // Precompute candidates
      val icCandidates = if (permittedIC) airports.filter(_.zone != baseAirport.zone).filter { airport =>
        countryRelationships.getOrElse((baseAirport.countryCode, airport.countryCode), 0) >= 3
      }.sortBy(- _.power) else List.empty
      val intlCandidates = airportsByZone(baseAirport.zone).filter(a => a.countryCode != baseAirport.countryCode && a.id != baseAirport.id).sortBy(- _.power)
      val domesticCandidates = airports.filter(a => a.countryCode == baseAirport.countryCode && a.id != baseAirport.id).sortBy(- _.power)
      
      // Determine counts, with adjustments for small domestic markets and city-states
      val cityStateIATAs = Set("SIN", "DXB", "AUH", "BAH", "DOH", "KWI", "HKG", "MFM")
      val isCityState = cityStateIATAs.contains(baseAirport.iata)
      var icCount = if (isCityState) 15 else 5
      var intlCount = if (baseAirport.size < 7) 10 else 20
      var domesticCount = if (baseAirport.size > 6) 10 else 20
      if (isCityState) {
        intlCount = 20
      }
      val domesticAirportCount = domesticCandidates.length
      if (domesticAirportCount < 7) {
        val reallocated = domesticCount - domesticAirportCount
        intlCount += reallocated
        domesticCount = domesticAirportCount
      }
      
      // Precompute max distances per category
      val maxDistByCategory = mutable.Map[String, Int]().withDefaultValue(0)
      if (permittedIC && icCandidates.nonEmpty) {
        val icToAirports = icCandidates.take(icCount)
        val maxIcDist = icToAirports.map(to => Computation.calculateDistance(baseAirport, to)).maxOption.getOrElse(0)
        if (maxDistByCategory("widebody") < maxIcDist) maxDistByCategory("widebody") = maxIcDist
      }
      val intlToAirports = intlCandidates.take(intlCount)
      intlToAirports.foreach { to =>
        val dist = Computation.calculateDistance(baseAirport, to)
        val cat = if (dist < 3000) "narrowbody" else if (dist < 5000) "narrowbody" else "widebody"
        if (maxDistByCategory(cat) < dist) maxDistByCategory(cat) = dist
      }
      val domToAirports = domesticCandidates.take(domesticCount)
      domToAirports.foreach { to =>
        val dist = Computation.calculateDistance(baseAirport, to)
        val cat = if (dist < 3000) "regional" else if (dist < 5000) "narrowbody" else "narrowbody"
        if (maxDistByCategory(cat) < dist) maxDistByCategory(cat) = dist
      }
      
      // Select families considering max distances
      val preferredFamilyByCategory = mutable.Map[String, String]()
      maxDistByCategory.foreach { case (cat, maxDist) =>
        val minRunway = baseAirport.runwayLength // conservative, assuming toAirport has at least this
        val validFamilies = familiesByCategory(cat).filter { fam =>
          modelsByFamily.getOrElse(fam, List.empty).exists(m => m.range >= maxDist && m.runwayRequirement <= minRunway)
        }
        if (validFamilies.nonEmpty) {
          preferredFamilyByCategory(cat) = Random.shuffle(validFamilies).head
        }
      }
      
      // IC Routes
      if (permittedIC && icCandidates.nonEmpty) {
        generateRoutes(newAirline, baseAirport, icCandidates.take(icCount), "ic", models, modelsByFamily, preferredFamilyByCategory, familiesByCategory, countryRelationships, baseAirport.runwayLength)
      }
      // International Routes
      if (intlCandidates.nonEmpty) {
        generateRoutes(newAirline, baseAirport, intlCandidates.take(intlCount), "intl", models, modelsByFamily, preferredFamilyByCategory, familiesByCategory, countryRelationships, baseAirport.runwayLength)
      }
      // Domestic Routes
      if (domesticCandidates.nonEmpty && domesticCount > 0) {
        generateRoutes(newAirline, baseAirport, domesticCandidates.take(domesticCount), "domestic", models, modelsByFamily, preferredFamilyByCategory, familiesByCategory, countryRelationships, baseAirport.runwayLength)
      }
    }
    Patchers.patchFlightNumber()
  }
 
  // This Actually Generates Routes and Picks Models:
  def generateRoutes(airline: Airline, fromAirport: Airport, toAirports: List[Airport], routeType: String, airplaneModels: List[Model], modelsByFamily: Map[String, List[Model]], preferredFamilyByCategory: mutable.Map[String, String], familiesByCategory: Map[String, List[String]], countryRelationships: Map[(String, String), Int], fromRunway: Int): Unit = {
    val newLinks = ListBuffer[Link]()
    toAirports.foreach { toAirport =>
      val relationship = countryRelationships.getOrElse((fromAirport.countryCode, toAirport.countryCode), 0)
      val estimatedOneWayDemandBusiness = DemandGenerator.computeDemandBetweenAirports(fromAirport, toAirport, relationship, PassengerType.BUSINESS)
      val estimatedOneWayDemandTourist = DemandGenerator.computeDemandBetweenAirports(fromAirport, toAirport, relationship, PassengerType.TOURIST)
      val estimatedOneWayDemand = estimatedOneWayDemandBusiness + estimatedOneWayDemandTourist
      val demandEcon = estimatedOneWayDemand(ECONOMY)
      if (demandEcon > 0) {
        val distance = Computation.calculateDistance(fromAirport, toAirport)
        var category = getCategory(distance)
        var freq = 21
        var factor = 1.1
        routeType match {
          case "ic" =>
            category = "widebody"
            freq = 14
            factor = 1.25
          case "intl" =>
            if (category == "regional") category = "narrowbody"
            freq = 21
            factor = 1.25
          case "domestic" =>
            if (category == "widebody") category = "narrowbody"
            freq = 21
            factor = 1.1
        }
        preferredFamilyByCategory.get(category) match {
          case Some(preferredFamily) =>
            val minRunway = math.min(fromRunway, toAirport.runwayLength)
            val availableModels = modelsByFamily.getOrElse(preferredFamily, List.empty).filter { model =>
              model.range >= distance && model.runwayRequirement <= minRunway
            }
            if (availableModels.nonEmpty) {
              val candidates = availableModels.sortBy(- _.capacity)
              var selectedFreq = freq
              var pickedModel = candidates.find { m =>
                m.capacity * selectedFreq <= demandEcon * factor
              }
              if (pickedModel.isEmpty && routeType == "domestic") {
                selectedFreq = 14
                pickedModel = candidates.find { m =>
                  m.capacity * selectedFreq <= demandEcon * factor
                }
              }
              if (pickedModel.isEmpty) {
                pickedModel = candidates.lastOption // smallest
              }
              pickedModel match {
                case Some(model) =>
                  val maxFrequencyPerAirplane = Computation.calculateMaxFrequency(model, distance)
                  if (maxFrequencyPerAirplane > 0) {
                    val frequency = selectedFreq
                    val airplanesRequired = math.ceil(frequency.toDouble / maxFrequencyPerAirplane).toInt
                    val assignedAirplanes = mutable.HashMap[Airplane, LinkAssignment]()
                    val flightMinutesRequired = Computation.calculateFlightMinutesRequired(model, distance)
                    var remainingFrequency = frequency
                    for (j <- 0 until airplanesRequired) {
                      val newAirplane = Airplane(model = model, owner = airline, constructedCycle = 0, purchasedCycle = 0, condition = Airplane.MAX_CONDITION, depreciationRate = 0, value = model.price)
                      AirplaneSource.saveAirplanes(List(newAirplane))
                      newAirplane.assignDefaultConfiguration()
                      val frequencyForThis = math.min(remainingFrequency, maxFrequencyPerAirplane)
                      val flightMinutesForThis = frequencyForThis * flightMinutesRequired
                      assignedAirplanes.put(newAirplane, LinkAssignment(frequencyForThis, flightMinutesForThis))
                      remainingFrequency -= frequencyForThis
                    }
                    if (remainingFrequency == 0) {
                      val flightType = Computation.getFlightType(fromAirport, toAirport, distance)
                      val price = Pricing.computeStandardPrice(distance, flightType, ECONOMY)
                      val capacity = LinkClassValues.getInstance(frequency * model.capacity)
                      val duration = Computation.calculateDuration(model, distance)
                      val newLink = Link(fromAirport, toAirport, airline, LinkClassValues.getInstance(price), distance, capacity, rawQuality = 40, duration = duration, frequency = frequency, flightType = flightType)
                      newLink.setAssignedAirplanes(assignedAirplanes.toMap)
                      newLinks += newLink
                    } else {
                      println(s"Cannot assign all frequency $frequency (remaining $remainingFrequency) for ${fromAirport.iata} to ${toAirport.iata} with max freq per plane $maxFrequencyPerAirplane")
                    }
                  } else {
                    println(s"Cannot fly even once from ${fromAirport.iata} to ${toAirport.iata} with model ${model.name}")
                  }
                case None =>
                  println(s"No suitable model for ${fromAirport.iata} to ${toAirport.iata}")
              }
            } else {
              println(s"No available models in family $preferredFamily for ${fromAirport.iata} to ${toAirport.iata}")
            }
          case None =>
            println(s"No preferred family for category $category for ${fromAirport.iata}")
        }
      }
    }
    LinkSource.saveLinks(newLinks.toList)
  }
 
  // Helper to determine aircraft category based on distance
  def getCategory(distance: Int): String = {
    if (distance < 3000) "regional"
    else if (distance < 5000) "narrowbody"
    else "widebody"
  }
}