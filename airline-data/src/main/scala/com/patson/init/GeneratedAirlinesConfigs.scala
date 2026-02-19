package com.patson.init

case class DeclaredRouteGroup(
  toIatas: List[String],
  frequency: Int,
  modelName: String,
  rawQuality: Int = 40
)

case class DeclaredBase(
  iata: String,
  scale: Int = 12,
  isHq: Boolean = false,
  routeGroups: List[DeclaredRouteGroup] = Nil
)

case class DeclaredAirline(
  name: String,
  username: String,
  bases: List[DeclaredBase],
  targetServiceQuality: Int = 30,
  initialBalance: Long = 0L
)

object GeneratedAirlinesConfigs {

  val airlineDeclarations: List[DeclaredAirline] = List(
/*
    // USA Airlines:
    // Air New England (BOS):
    DeclaredAirline(
      name = "BOT Air New England",
      username = "AirNewEngland",
      bases = List(
        DeclaredBase("BOS", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        ))
      )
    ),
    // PanAmerican World Airlines (JFK, MIA, LAX):
    DeclaredAirline(
      name = "BOT PanAmerican World Airways",
      username = "PanAmerican",
      bases = List(
        DeclaredBase("JFK", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        )),
        DeclaredBase("MIA", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        )),
        DeclaredBase("LAX", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        ))
      )
    ),
    // TWA - TransWorldAirlines (JFK, STL, MCI):
    DeclaredAirline(
      name = "BOT TWA",
      username = "TransWorldAirlines",
      bases = List(
        DeclaredBase("JFK", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        )),
        DeclaredBase("STL", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        )),
        DeclaredBase("MCI", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        ))
      )
    ),
    // North-East Airlines (LGA):
    DeclaredAirline(
      name = "BOT North East Airlines",
      username = "NorthEastAirlines",
      bases = List(
        DeclaredBase("LGA", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        ))
      )
    ),
    // Capital Airlines (PHL, PIT):
    DeclaredAirline(
      name = "BOT Capital Airlines",
      username = "CapitalAirlines",
      bases = List(
        DeclaredBase("PHL", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        )),
        DeclaredBase("PIT", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        ))
      )
    ),
    // National Airlines (DCA, IAD, TPA, MSY):
    DeclaredAirline(
      name = "BOT National Airlines",
      username = "NationalAirlines",
      bases = List(
        DeclaredBase("DCA", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        )),
        DeclaredBase("IAD", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        )),
        DeclaredBase("TPA", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        )),
        DeclaredBase("MSY", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        )),
      )
    ),
    // Allegheney Airlines (BWI):
    DeclaredAirline(
      name = "BOT Allegheney Airlines",
      username = "AllegheneyAirlines",
      bases = List(
        DeclaredBase("BWI", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        ))
      )
    ),
    // Piedmont Airlines (CLT, RDU):
    DeclaredAirline(
      name = "BOT Piedmont Airlines",
      username = "PiedmontAirlines",
      bases = List(
        DeclaredBase("CLT", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        )),
        DeclaredBase("RDU", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        ))
      )
    ),
    // Delta Airlines (ATL, LGA):
    DeclaredAirline(
      name = "BOT Delta Airlines",
      username = "DeltaAirlines",
      bases = List(
        DeclaredBase("ATL", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        )),
        DeclaredBase("LGA", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        )),
      )
    ),
    // Eastern Airlines (MIA):
    DeclaredAirline(
      name = "BOT Eastern Airlines",
      username = "EasternAirlines",
      bases = List(
        DeclaredBase("MIA", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        ))
      )
    ),
    // Hawaiian Airlines (HNL):
    DeclaredAirline(
      name = "BOT Hawaiian Airlines",
      username = "HawaiianAirlines",
      bases = List(
        DeclaredBase("HNL", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        ))
      )
    ),
    // South-East Airlines (BNA):
    DeclaredAirline(
      name = "BOT Southeast Airlines",
      username = "SouthEastAirlines",
      bases = List(
        DeclaredBase("BWI", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        ))
      )
    ),
    // Mohawk Airlines (CLE):
    DeclaredAirline(
      name = "BOT Mohawk Airlines",
      username = "MohawkAirlines",
      bases = List(
        DeclaredBase("BWI", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        ))
      )
    ),
    // North-West Oriental Airlines (DTW, MSP):
    DeclaredAirline(
      name = "BOT Northwest Oriental Airlines",
      username = "NorthwestOrientalAirlines",
      bases = List(
        DeclaredBase("CLT", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        )),
        DeclaredBase("RDU", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        ))
      )
    ),
    // United Airlines (ORD, IAH, EWR):
    DeclaredAirline(
      name = "BOT Allegheney Airlines",
      username = "AllegheneyAirlines",
      bases = List(
        DeclaredBase("ORD", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        )),
        DeclaredBase("IAH", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        )),
        DeclaredBase("DEN", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        ))
      )
    ),
    // Ozark Airlines (STL):
    DeclaredAirline(
      name = "BOT Ozark Airlines",
      username = "OzarkAirlines",
      bases = List(
        DeclaredBase("BWI", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        ))
      )
    ),
    // Branif Airlines (DAL, HOU, AUS):
    DeclaredAirline(
      name = "BOT Braniff Airlines",
      username = "BraniffAirlines",
      bases = List(
        DeclaredBase("DAL", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        )),
        DeclaredBase("AUS", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        ))
      )
    ),
    // American Airlines (DFW, DCA, PHX):
    DeclaredAirline(
      name = "BOT American Airlines",
      username = "AmericanAirlines",
      bases = List(
        DeclaredBase("DFW", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        )),
        DeclaredBase("DCA", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        )),
        DeclaredBase("PHX", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        ))
      )
    ),
    // Bonanza Airlines (LAS, PHX, LGB):
    DeclaredAirline(
      name = "BOT American Airlines",
      username = "AmericanAirlines",
      bases = List(
        DeclaredBase("DFW", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        )),
        DeclaredBase("DCA", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        )),
        DeclaredBase("PHX", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        ))
      )
    ),
    // American Pacific Airlines (LAX, SFO, MCI):
    DeclaredAirline(
      name = "BOT American Pacific Airlines",
      username = "AmericanPacificAirlines",
      bases = List(
        DeclaredBase("SFO", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        )),
        DeclaredBase("LAX", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        )),
        DeclaredBase("MCI", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        ))
      )
    ),
    // Mountain West Airlines (SLC, SEA, OAK):
    DeclaredAirline(
      name = "BOT Mountain West Airlines",
      username = "MountainWestAirlines",
      bases = List(
        DeclaredBase("SLC", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        )),
        DeclaredBase("OAK", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        )),
        DeclaredBase("SEA", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        ))
      )
    ),

    // Canadian Airlines:
    // Canadian Pacific Airlines:
    DeclaredAirline(
      name = "BOT Canadian Paicifc Airlines",
      username = "CanadianPacifcAirlines",
      bases = List(
        DeclaredBase("YVR", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        )),
        DeclaredBase("YEG", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        )),
        DeclaredBase("YYC", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        ))
      )
    ),
    // Pacific Western Airlines:
    DeclaredAirline(
      name = "BOT Pacific Western Airlines",
      username = "PacificWesternAirlines",
      bases = List(
        DeclaredBase("YEG", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        )),
        DeclaredBase("YYC", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        ))
      )
    ),
    // Quebecair (YQB, YHZ):
    DeclaredAirline(
      name = "BOT Quebecair",
      username = "Quebecair",
      bases = List(
        DeclaredBase("YEG", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        )),
        DeclaredBase("YYC", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        ))
      )
    ),
    // TransCanada Airlines (YYZ, YUL):
    DeclaredAirline(
      name = "BOT Pacific Western Airlines",
      username = "PacificWesternAirlines",
      bases = List(
        DeclaredBase("YYZ", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        )),
        DeclaredBase("YUL", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        ))
      )
    ),

    // Mexico Airlines:
    // AeroNorte (MTY):
    DeclaredAirline(
      name = "BOT Quebecair",
      username = "Quebecair",
      bases = List(
        DeclaredBase("MTY", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        ))
      )
    ),
    // Mexicana de Aviacion (MEX, GDL):
    DeclaredAirline(
      name = "BOT Quebecair",
      username = "Quebecair",
      bases = List(
        DeclaredBase("MEX", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        )),
        DeclaredBase("GDL", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        ))
      )
    ),

    // LATAM Airlines:
    // Aerolineas Argentinas (EZE, AEP):
    DeclaredAirline(
      name = "BOT Aerolineas Argentinas",
      username = "AerolineasArgentinas",
      bases = List(
        DeclaredBase("EZE", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        )),
        DeclaredBase("AEP", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        ))
      )
    ),
    // Avianca (BOG):
    DeclaredAirline(
      name = "BOT Avianca",
      username = "Avianca",
      bases = List(
        DeclaredBase("BOG", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        ))
      )
    ),
    // VARIG (GRU, GIG, CGH, BSB):
    DeclaredAirline(
      name = "BOT Aerolineas Argentinas",
      username = "AerolineasArgentinas",
      bases = List(
        DeclaredBase("GRU", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        )),
        DeclaredBase("GIG", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        )),
        DeclaredBase("CGH", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        ))
        DeclaredBase("BSB", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        ))
      )
    ),
    // VASP (GRU):
    DeclaredAirline(
      name = "BOT Avianca",
      username = "Avianca",
      bases = List(
        DeclaredBase("BOG", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        ))
      )
    ),
    // Cruzeiro do Sul (GIG, SDU):
    DeclaredAirline(
      name = "BOT Avianca",
      username = "Avianca",
      bases = List(
        DeclaredBase("BOG", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        ))
      )
    ),

    // Pacific Asia Bots:
    // All Nippon Airways (HND, NRT, KIX):
    DeclaredAirline(
      name = "BOT Aerolineas Argentinas",
      username = "AerolineasArgentinas",
      bases = List(
        DeclaredBase("HND", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        )),
        DeclaredBase("NRT", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        )),
        DeclaredBase("KIX", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        ))
      )
    ),
    // Japan Airlines (HND, ITM, KIX, FUK):
    DeclaredAirline(
      name = "BOT Aerolineas Argentinas",
      username = "AerolineasArgentinas",
      bases = List(
        DeclaredBase("HND", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        )),
        DeclaredBase("ITM", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        )),
        DeclaredBase("KIX", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        )),
        DeclaredBase("FUK", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        )),
      )
    ),
    // Korean Airlines (ICN, GMP, PUS):
    DeclaredAirline(
      name = "BOT Aerolineas Argentinas",
      username = "AerolineasArgentinas",
      bases = List(
        DeclaredBase("ICN", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        )),
        DeclaredBase("GMP", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        )),
        DeclaredBase("PUS", 7, true, List(
          DeclaredRouteGroup(List("LAX", "SFO", "SEA"), 14, "Boeing 777-300ER"),
          DeclaredRouteGroup(List("BOS", "IAD", "PHL"), 24, "Boeing 737-800")
        ))
      )
    ),
*/



    // Chinese Airlines:
    // Air China (PEK, PKX, HRB, XYI):

    // China Eastern Airlines (PVG, SHA, PEK):

    // China Southern Airlines (CAN, SZX, PVG):

    // Shanghai Airlines (SHA, PVG):

    // Sichuan Airlines (CTU, KMG):
    
    // Wuhan Airlines (WUH): 
    

    // Add the rest of your 250+ airlines here using the same pattern.
  )
}