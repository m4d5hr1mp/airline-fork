/*// New separate file: AirlineConfigs.scala
package com.patson.init

object AirlineConfigs {
  case class RouteConfig(toIata: String, frequency: Int, family: String)

  case class AirlineConfig(
    iata: String,
    airlineName: String,
    username: String,
    password: String = "1234",
    baseLevel: Int = 10,
    families: Set[String],
    routes: List[RouteConfig]
  )

  val airlineConfigs: List[AirlineConfig] = List(
    //BOS-1 Intl + Eastern + Regionals
    AirlineConfig(
      iata = "BOS",
      airlineName = "BOT Boston Primary",
      username = "BOS1",
      // E-Jets, MD-90's
      families = Set(),
      routes = List(
        RouteConfig("LHR", 14, "Boeing 767"),
        RouteConfig("MAN", 14, "Boeing 767"),
        RouteConfig("DUB", 14, "Boeing 767"),
        RouteConfig("CDG", 14, "Boeing 767"),
        RouteConfig("FRA", 14, "Boeing 767"),
        RouteConfig("NRT", 7, "Boeing 767"),
        RouteConfig("ICN", 7, "Boeing 767"),

        RouteConfig("YYZ", 28, "Airbus A320"),
        RouteConfig("YUL", 21, "Airbus A320"),
        RouteConfig("YOW", 14, "Airbus A320"),
        RouteConfig("YQB", 14, "Airbus A320"),
        RouteConfig("MEX", 21, "Airbus A320"),
        RouteConfig("CUN", 14, "Airbus A320"),

        RouteConfig("SJU", 14, "Airbus A320"),
        RouteConfig("KIN", 14, "Airbus A320"),

        // Domestic High-Frequency:
        RouteConfig("JFK", 42, "Airbus A320"),
        RouteConfig("LGA", 28, "Airbus A320"),
        RouteConfig("PHL", 36, "Airbus A320"),
        RouteConfig("DCA", 32, "Airbus A320"),
        RouteConfig("BWI", 36, "Airbus A320"),

        RouteConfig("RDU", 28, "Airbus A320"),
        RouteConfig("ORF", 21, "Airbus A320"),
        RouteConfig("CLT", 36, "Airbus A320"),
        RouteConfig("JAX", 14, "Airbus A320"),
        RouteConfig("FLL", 21, "Airbus A320"),
        RouteConfig("MCO", 21, "Airbus A320"),
        RouteConfig("MIA", 28, "Airbus A320"),
        RouteConfig("TPA", 21, "Airbus A320"),

        RouteConfig("ATL", 36, "Airbus A320"),
        RouteConfig("BNA", 28, "Airbus A320"),
        RouteConfig("MSY", 21, "Airbus A320"),
        RouteConfig("MEM", 21, "Airbus A320"),

        RouteConfig("BUF", 21, "Airbus A320"),
        RouteConfig("CLE", 21, "Airbus A320"),
        RouteConfig("DTW", 28, "Airbus A320"),
        RouteConfig("CMH", 21, "Airbus A320"),
        RouteConfig("CVG", 21, "Airbus A320"),
        RouteConfig("SDF", 21, "Airbus A320"),
        RouteConfig("IND", 21, "Airbus A320"),
        RouteConfig("MDW", 28, "Airbus A320"),
        RouteConfig("ORD", 28, "Airbus A320"),
        RouteConfig("MKE", 21, "Airbus A320"),
        RouteConfig("MSP", 28, "Airbus A320"),
        RouteConfig("STL", 21, "Airbus A320"),

        RouteConfig("DFW", 28, "Airbus A320"),
        RouteConfig("AUS", 21, "Airbus A320"),
        RouteConfig("IAH", 28, "Airbus A320"),

        RouteConfig("DEN", 28, "Airbus A320"),
        RouteConfig("SLC", 14, "Airbus A320"),
        RouteConfig("PHX", 21, "Boeing 767"),
        RouteConfig("LAS", 21, "Boeing 767"),

        RouteConfig("SFO", 14, "Boeing 767"),
        RouteConfig("SJC", 21, "Boeing 767"),
        RouteConfig("LAX", 7,  "Boeing 767"),
        RouteConfig("LGB", 21, "Boeing 767"),

        RouteConfig("SEA", 14, "Boeing 767"),
        RouteConfig("PDX", 14, "Boeing 767"),

        RouteConfig("PWM", 14, "Bombardier CRJ"),
        RouteConfig("BGR", 14, "Bombardier CRJ"),
        RouteConfig("PBG", 14, "Bombardier CRJ"),
        RouteConfig("ALB", 14, "Bombardier CRJ"),
        RouteConfig("ROC", 14, "Bombardier CRJ"),
        RouteConfig("ACY", 21, "Bombardier CRJ"),
        RouteConfig("AVP", 14, "Bombardier CRJ"),
        RouteConfig("MDT", 14, "Bombardier CRJ"),
        RouteConfig("CAK", 14, "Bombardier CRJ"),
        RouteConfig("BDL", 21, "Bombardier CRJ"),
      )
    ),

    // JFK-1 INTL + Majors
    AirlineConfig(
      iata = "JFK",
      airlineName = "BOT-JFK-Primary",
      username = "JFK1",
      families = Set("A320", "A330", "A340", "A380"),
      routes = List(
        RouteConfig("LHR", 28, "Boeing 767"),
        RouteConfig("MAN", 21, "Boeing 767"),
        RouteConfig("DUB", 21, "Boeing 767"),
        RouteConfig("CDG", 28, "Boeing 767"),
        RouteConfig("FRA", 28, "Boeing 767"),
        RouteConfig("MUC", 28, "Boeing 767"),
        RouteConfig("HND", 14, "Boeing 747"),
        RouteConfig("ICN", 7, "Boeing 747"),
        RouteConfig("HKG", 7, "Boeing 747"),
        RouteConfig("PVG", 7, "Boeing 747"),
        RouteConfig("PKX", 7, "Boeing 747"),

        RouteConfig("BOS", 28, "McDonnell Douglas MD-90"),
        RouteConfig("PHL", 28, "McDonnell Douglas MD-90"),
        RouteConfig("BWI", 28, "McDonnell Douglas MD-90"),
        RouteConfig("IAD", 28, "McDonnell Douglas MD-90"),
        RouteConfig("DCA", 28, "McDonnell Douglas MD-90"),
        RouteConfig("CLT", 28, "McDonnell Douglas MD-90"),
        RouteConfig("ATL", 28, "McDonnell Douglas MD-90"),
        RouteConfig("MIA", 28, "McDonnell Douglas MD-90"),
        RouteConfig("TPA", 28, "McDonnell Douglas MD-90"),
        RouteConfig("BNA", 28, "McDonnell Douglas MD-90"),
        RouteConfig("DTW", 28, "McDonnell Douglas MD-90"),
        RouteConfig("MDW", 28, "McDonnell Douglas MD-90"),
        RouteConfig("ORD", 28, "McDonnell Douglas MD-90"),
        RouteConfig("MSP", 28, "McDonnell Douglas MD-90"),
        RouteConfig("DFW", 28, "McDonnell Douglas MD-90"),
        RouteConfig("AUS", 28, "McDonnell Douglas MD-90"),
        RouteConfig("IAH", 28, "McDonnell Douglas MD-90"),
        RouteConfig("DEN", 28, "Boeing 767"),
        RouteConfig("PHX", 14, "Boeing 767"),
        RouteConfig("LAS", 21, "Boeing 767"),
        RouteConfig("SFO", 28, "Boeing 767"),
        RouteConfig("LAX", 28, "Boeing 767"),
        RouteConfig("SEA", 14, "Boeing 767"),
      )
    ),

  )
}*/