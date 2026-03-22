package com.patson.model.airplane

import com.patson.ChronologyConverter._

object ModelAvailability {
<<<<<<< HEAD

  // ===================================================================
  // ORIGINAL MAP – kept with exact original name so Model.scala compiles
  // Values = chronological weeks after 1 January 1958
  // ===================================================================
  val modelAvailabilityCycles: Map[String, Int] = Map(
=======
  // Chronological weeks from WORLD_START_YEAR (1955).
  // 0 = available at game start. Max = 3900 (Week 1, 2030).
  val modelAvailabilityWeeks: Map[String, Int] = Map(
>>>>>>> b3687362a7034d069cb08f3dd335716a6503b9d4
    "Vickers Viscount 700" -> 0,
    "Lisunov Li-2" -> 0,
    "Douglas DC-3" -> 0,
    "Lockheed L-749 Constellation" -> 0,
    "Lockheed L-749A Constellation" -> 0,
    "Lockheed L-1049 Super Constellation" -> 0,
    "Lockheed L-1049C Super Constellation" -> 0,
    "Lockheed L-1049G Super Constellation" -> 0,
    "Lockheed L-1649 Starliner" -> 0,
    "Douglas DC-6" -> 0,
    "Douglas DC-6B" -> 0,
    "Douglas DC-7" -> 0,
    "Douglas DC-7C" -> 0,
    "Boeing 377 Stratocruiser" -> 0,
    "Convair CV-240" -> 0,
    "Convair CV-340" -> 0,
    "Convair CV-440" -> 0,
    "Vickers Viscount 800" -> 0,
    "Vickers Viscount 810" -> 0,
    "Vickers Viking" -> 0,
    "Ilyushin Il-18" -> 0,
    "Antonov An-10" -> 0,
    "Antonov An-24" -> 0,
    "Xian Y-7" -> 0,
    "Handley Page HP81 Hermes" -> 0,
    "Boeing 707-120" -> 204,
    "DeHaviland Comet 4" -> 204,
    "Sud-Aviation Caravelle I" -> 233,
    "Boeing 707-320" -> 252,
    "Douglas DC-8-10" -> 257,
    "Douglas DC-8-21" -> 271,
    "Douglas DC-8-33" -> 290,
    "DeHaviland Comet 4B" -> 290,
    "Convair CV-880" -> 295,
    "Convair CV-880M" -> 295,
    "Sud-Aviation Caravelle III" -> 295,
    "Boeing 720" -> 305,
    "DeHaviland Comet 4C" -> 314,
    "Boeing 707-120B" -> 338,
    "Boeing 720B" -> 338,
    "Sud-Aviation Caravelle VI" -> 358,
    "Convair CV-990A" -> 396,
    "Boeing 707-320B" -> 410,
    "Tupolev Tu-124" -> 430,
    "Douglas DC-8-53" -> 473,
    "Boeing 727-100" -> 516,
    "Vickers VC-10 Type 1101" -> 521,
    "Sud-Aviation Caravelle 10B" -> 540,
    "BAC One-Eleven 200" -> 574,
    "Douglas DC-9-10" -> 612,
    "BAC One-Eleven 300" -> 665,
    "Douglas DC-9-30" -> 679,
    "Douglas DC-8-61" -> 679,
    "Douglas DC-8-62" -> 689,
    "Douglas DC-8-63" -> 703,
    "Tupolev Tu-134" -> 713,
    "Ilyushin Il-62" -> 713,
    "Boeing 727-200" -> 727,
    "Douglas DC-9-40" -> 742,
    "Boeing 737-100" -> 746,
    "Boeing 737-200" -> 751,
    "BAC One-Eleven 500" -> 780,
    "Fokker F28 Fellowship" -> 799,
    "Boeing 747-100" -> 852,
    "Boeing 747-200" -> 919,
    "Douglas DC-10-10" -> 948,
    "Tupolev Tu-154" -> 977,
    "Lockheed L-1011-1" -> 991,
    "Douglas DC-10-30" -> 1030,
    "Douglas DC-10-40" -> 1039,
    "Tupolev Tu-154A" -> 1087,
    "Ilyushin Il-62M" -> 1092,
    "Airbus A300B2-100" -> 1111,
    "Lockheed L-1011-100" -> 1145,
    "Airbus A300B4-200" -> 1150,
    "Tupolev Tu-154B" -> 1154,
    "Airbus A300B2-200" -> 1159,
    "Airbus A300B4-100" -> 1174,
    "Douglas DC-9-50" -> 1183,
    "Boeing 747SP" -> 1222,
    "Lockheed L-1011-200" -> 1265,
    "Lockheed L-1011-500" -> 1399,
    "McDonell-Douglas MD-81" -> 1471,
    "Ilyushin Il-86" -> 1481,
    "Yakovlev Yak-42" -> 1495,
    "Boeing 767-200" -> 1582,
    "Douglas DC-8-71" -> 1586,
    "Douglas DC-8-72" -> 1596,
    "Boeing 757-200" -> 1601,
    "Boeing 757-200 IGW" -> 1601,
    "BAe-146-200" -> 1606,
    "Douglas DC-8-73" -> 1610,
    "Boeing 747-300" -> 1610,
    "Airbus A310-300" -> 1615,
    "BAe-146-100" -> 1620,
    "Boeing 767-200ER" -> 1668,
    "Tupolev Tu-154M" -> 1673,
    "Airbus A300B4-600" -> 1682,
    "Saab 340" -> 1682,
    "Bombardier DHC-8-100" -> 1702,
    "Boeing 737-500" -> 1706,
    "McDonell-Douglas MD-83" -> 1721,
    "Embraer EMB120 Brasilia" -> 1754,
    "ATR 42-600" -> 1764,
    "ATR 42-300" -> 1764,
    "Airbus A310-200" -> 1778,
    "Boeing 767-300" -> 1817,
    "SAIC MD-81" -> 1860,
    "Fokker 50" -> 1860,
    "McDonell-Douglas MD-87" -> 1879,
    "Fokker 100" -> 1894,
    "Airbus A320-100" -> 1894,
    "Boeing 767-300ER" -> 1898,
    "Boeing 737-400" -> 1927,
    "SAIC MD-83" -> 1932,
    "Bombardier DHC-8-200" -> 1951,
    "Boeing 747-400" -> 1951,
    "BAe-146-300" -> 1994,
    "Boeing 737-300" -> 2009,
    "McDonnell-Douglas MD-11" -> 2052,
    "SAIC MD-87" -> 2105,
    "Airbus A340-300" -> 2182,
    "Airbus A340-200" -> 2182,
    "Ilyushin Il-96-400" -> 2201,
    "Avro RJ85" -> 2225,
    "Avro RJ70" -> 2225,
    "Avro RJ100" -> 2225,
    "Airbus A330-300" -> 2230,
    "Airbus A321-100" -> 2234,
    "Bombardier CRJ200" -> 2239,
    "Saab 2000" -> 2273,
    "McDonnell-Douglas MD-11ER" -> 2287,
    "Fokker 70ER" -> 2297,
    "Fokker 70" -> 2297,
    "Bombardier DHC-8-300" -> 2302,
    "McDonnell Douglas MD-90-30" -> 2302,
    "Boeing 777-200" -> 2311,
    "ATR 42-500" -> 2345,
    "Tupolev Tu-204-100" -> 2350,
    "Airbus A319-100" -> 2364,
    "Airbus A320-200" -> 2374,
    "ATR 72-600" -> 2402,
    "Boeing 777-200ER" -> 2407,
    "Embraer ERJ145" -> 2417,
    "Airbus A321-200" -> 2417,
    "McDonnell Douglas MD-90-30ER" -> 2450,
    "Boeing 737-700" -> 2455,
    "Airbus A330-200" -> 2474,
    "Boeing 737-800" -> 2474,
    "Boeing 777-300" -> 2479,
    "Boeing 737-600" -> 2498,
    "Boeing 757-300" -> 2527,
    "Embraer ERJ135" -> 2546,
    "McDonnell Douglas MD-90-40" -> 2546,
    "McDonnell Douglas MD-95" -> 2561,
    "Bombardier DHC-8-400" -> 2575,
    "SAIC MD-90-30" -> 2614,
    "Boeing 767-400ER" -> 2614,
    "Bombardier CRJ700" -> 2638,
    "McDonnell Douglas MD-90-55" -> 2642,
    "SAIC MD-90-30ER" -> 2652,
    "Boeing 737-900" -> 2652,
    "Embraer ERJ140" -> 2662,
    "Airbus A340-600" -> 2724,
    "Embraer ERJ145XR" -> 2738,
    "SAIC MD-90-40" -> 2738,
    "Boeing 747-400ER" -> 2738,
    "Bombardier CRJ900" -> 2753,
    "Airbus A318" -> 2782,
    "Airbus A340-500" -> 2796,
    "Embraer EMB170" -> 2815,
    "Boeing 777-300ER" -> 2820,
    "SAIC MD-90-55" -> 2839,
    "Tupolev Tu-204-300" -> 2882,
    "Embraer EMB175" -> 2892,
    "Embraer EMB190" -> 2902,
    "Boeing 777-200LR" -> 2930,
    "Embraer EMB195" -> 2959,
    "Boeing 737-700ER" -> 2983,
    "Boeing 737-900ER" -> 2993,
    "Comac ARJ21" -> 3002,
    "Airbus A380-800" -> 3022,
    "Ilyushin Il-96-300" -> 3108,
    "Bombardier DHC-6-400" -> 3180,
    "Bombardier CRJ1000" -> 3204,
    "Sukhoi Superjet 100-95B" -> 3223,
    "ATR 72-500" -> 3238,
    "Boeing 787-8 Dreamliner" -> 3257,
    "Boeing 747-8i" -> 3290,
    "Sukhoi Superjet 100-95LR" -> 3319,
    "Boeing 787-9 Dreamliner" -> 3420,
    "Airbus A350-900" -> 3444,
    "Airbus A320neo" -> 3497,
    "Irkut MC-21-300" -> 3506,
    "Comac C919-700" -> 3516,
    "Bombardier CS100" -> 3530,
    "Bombardier CS300" -> 3554,
    "Irkut MC-21-200" -> 3554,
    "Airbus A321neo" -> 3574,
    "Boeing 737 MAX 8" -> 3574,
    "Comac C919-700ER" -> 3593,
    "Irkut MC-21-100" -> 3612,
    "Airbus A350-1000" -> 3622,
    "Boeing 737 MAX 9" -> 3622,
    "Embraer E190-E2" -> 3631,
    "Boeing 787-10 Dreamliner" -> 3631,
    "Airbus A321neoLR" -> 3655,
    "Airbus A350-900ULR" -> 3660,
    "Airbus A330-900neo" -> 3665,
    "Airbus A319neo" -> 3670,
    "Boeing 737 MAX 7" -> 3670,
    "Embraer E195-E2" -> 3684,
    "Comac C919-600" -> 3689,
    "Airbus A330-800neo" -> 3698,
    "Boeing 737 MAX 10" -> 3727,
    "Boeing 777-9" -> 3727,
    "Comac C919-800" -> 3775,
    "Boeing 737 MAX 8-200" -> 3809,
    "Embraer E175-E2" -> 3823,
    "Boeing 777-8" -> 3842,
    "Airbus A321neoXLR" -> 3900
  )

<<<<<<< HEAD
  // ===================================================================
  // CORRECT RELEASE CYCLE CALCULATION (weeks after 1 Jan 1958)
  // ===================================================================
  private val CYCLES_TO_JAN_1958: Int = 3 * cyclesPerYear                    // 720
  private val CYCLES_PER_WEEK: Double = cyclesPerYear.toDouble / 52

  def getAvailabilityCycle(name: String): Int = {
    val weeksAfter1958 = modelAvailabilityCycles.getOrElse(name, 0)

    if (weeksAfter1958 <= 0) {
      0                                           // Pre-1958 models available from game start
    } else {
      CYCLES_TO_JAN_1958 + math.ceil(weeksAfter1958 * CYCLES_PER_WEEK).toInt
    }
  }

  private val MONTH_NAMES = Vector(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
  )

  /** Accurate release year (matches top-bar date display) */
  def getReleaseYear(modelName: String): Int = {
    val releaseCycle = getAvailabilityCycle(modelName)
    val yearsPassed = releaseCycle / cyclesPerYear
    WORLD_START_YEAR + yearsPassed
  }

  /** Display string identical to the top bar (e.g. "March 1958") */
  def getReleaseGameDate(modelName: String): String = {
    val releaseCycle = getAvailabilityCycle(modelName)
    val yearsPassed = releaseCycle / cyclesPerYear
    val year = WORLD_START_YEAR + yearsPassed
    val cyclesInYear = releaseCycle % cyclesPerYear
    val monthIndex = cyclesInYear / cyclesPerChronologicalMonth
    s"${MONTH_NAMES(monthIndex)} $year"
=======
  /** Game cycle on which this model becomes available */
  def getAvailabilityCycle(name: String): Int =
    modelAvailabilityWeeks.getOrElse(name, 0) * cyclesPerChronologicalWeek

  /** In-game year of release */
  def getReleaseYear(modelName: String): Int = {
    val weeks = modelAvailabilityWeeks.getOrElse(modelName, 0)
    WORLD_START_YEAR + weeks / WEEKS_PER_YEAR
  }

  /** Display string e.g. "Week 21, 1971" */
  def getReleaseGameDate(modelName: String): String = {
    val weeks = modelAvailabilityWeeks.getOrElse(modelName, 0)
    val year       = WORLD_START_YEAR + weeks / WEEKS_PER_YEAR
    val weekOfYear = (weeks % WEEKS_PER_YEAR) + 1
    s"Week $weekOfYear, $year"
>>>>>>> b3687362a7034d069cb08f3dd335716a6503b9d4
  }
}