package com.patson.init

import com.patson.data.CountrySource

import scala.collection.mutable
import scala.collection.mutable.Map

object CountryMutualRelationshipGenerator extends App {
  /**
   * - Affliation is mutal between all members. Affliations only "upgrade" relations, never decrease
   *
   * Some relations set in the computation function!
   */
  // ---------------------------------------------------------------------------
  // 1955 world-start blocs
  // ---------------------------------------------------------------------------

  // OEEC (precursor to OECD) — Jan 1 1955 members
  lazy val OEEC_1955 = List("AT","BE","DK","FR","DE","GR","IS","IE","IT","LU","NL","NO","PT","SE","CH","TR","GB")

  // NATO — Jan 1 1955 members (DE joins May 1955, handled by chronological patcher)
  lazy val NATO_1955 = List("US","CA","GB","FR","BE","NL","DK","NO","IS","IT","PT","LU","GR","TR")

  // Baghdad Pact — full member list (signed Feb 1955, US is backer but not a member)
  lazy val BAGHDAD_PACT = List("IQ","TR","IR","PK","GB")

  // SEATO — Jan 1 1955 members
  lazy val SEATO_1955 = List("US","GB","FR","AU","NZ","PH","TH","PK")

  // Commonwealth Anglo Dominions — fully sovereign, cultural/linguistic unity with GB
  // ZA included: white-minority governed in 1955
  lazy val COMMONWEALTH_ANGLO_DOMINIONS = List("GB","CA","AU","NZ","ZA","HK")

  // Non-Anglo Dominions — sovereign, border exists, handled via FRIENDSHIPS
  lazy val COMMONWEALTH_NON_ANGLO_DOMINIONS = List("IN","PK","LK")

  // Self-governing colonies — substantial autonomy, not yet independent
  lazy val EMPIRE_SELF_GOV_ZW = List("ZW")          // Rhodesia — Anglo settlers, +4 with GB
  lazy val EMPIRE_SELF_GOV    = List("MY","SG")     // Malaya/Singapore — +3 with GB

  // Crown colonies and protectorates — directly governed by the Crown
  lazy val EMPIRE_CROWN_COLONIES = List(
    "KE","UG","TZ","ZM","MW","NG","GH","SL","GM","BW","LS","SZ",  // Africa
    "JM","TT","BB","GY","BZ",                                       // Caribbean
    "FJ","SB","PG",                                                 // Pacific
    "CY","MT"                                                       // Mediterranean
  )

  // Domestic markets — no customs border +5
  lazy val USA_DOMESTIC = List("US","PR","VI","GU","AS","MP","MH","FM","PW")
  lazy val GB_DOMESTIC  = List("GB","GI","FK","SH","TC","KY","BM","VG","MS","AI","IM","JE","GG")
  lazy val FR_DOMESTIC  = List("FR","GF","GP","MQ","RE","PM","BL","MF","NC","PF","WF","YT","TF")
  lazy val NL_DOMESTIC  = List("NL","AW","CW","SX","BQ")

  // AU domestic territories — Norfolk, Christmas, Cocos Islands
  lazy val AU_DOMESTIC = List("AU","NF","CX","CC")

  // NZ domestic territories
  lazy val NZ_DOMESTIC = List("NZ","CK","NU","TK")

  // Nauru — joint AU/NZ/GB mandate
  lazy val NAURU_MANDATE = List("AU","NZ","GB","NR")

  // Papua New Guinea — Australian administered, +4 (not domestic)
  lazy val AU_PNG = List("AU","PG")

  // Western Samoa — NZ mandate until 1962, +4 (not domestic)
  lazy val NZ_SAMOA = List("NZ","WS")

  // ANZUS — mutual defence treaty 1951
  lazy val ANZUS = List("US","AU","NZ")

  // ---------------------------------------------------------------------------
  // Communist bloc
  // ---------------------------------------------------------------------------

  // USSR domestic — all successor state codes, single customs/travel space
  lazy val USSR_DOMESTIC = List("RU","UA","BY","KZ","UZ","TJ","TM","KG","AZ","AM","GE","EE","LV","LT","MD")

  // Yugoslavia domestic — single nation under Tito
  lazy val YUGO_DOMESTIC = List("RS","HR","BA","SI","MK","ME")

  // Czechoslovakia domestic — single nation
  lazy val CZECHO_DOMESTIC = List("CZ","SK")

  // Warsaw Pact — USSR bloc + satellites (AL was founding member in 1955)
  lazy val WARSAW_PACT = List("RU","UA","BY","KZ","UZ","TJ","TM","KG","AZ","AM","GE","EE","LV","LT","MD",
                               "PL","CZ","SK","HU","RO","BG","AL")

  // ---------------------------------------------------------------------------
  // Non-Aligned Movement — Bandung 1955
  // ---------------------------------------------------------------------------

  // NAM inner circle — Tito, Nehru, Nasser, Sukarno, Nkrumah
  lazy val NAM_CORE = List("RS","IN","EG","ID","GH")

  // French protectorates in MENA — nominally sovereign, France controls foreign policy
  lazy val FR_MENA_PROTECTORATES = List("MA","TN")

  // French Sub-Saharan Africa — full colonies, franc zone
  lazy val FR_AFRICA = List(
    "SN","ML","GN","CI","BF","NE","BJ","TD","CF","CG","GA","CM","DJ","MG","KM","TG"
  )

  // ---------------------------------------------------------------------------

  lazy val AFFILIATIONS = List(

    // --- 1955 world-start blocs ---

    Affiliation("OEEC", 2, OEEC_1955),

    Affiliation("NATO 1955", 3, NATO_1955),

    Affiliation("Baghdad Pact", 2, BAGHDAD_PACT),

    Affiliation("SEATO", 1, SEATO_1955),

    // Anglo Dominions + HK — full cultural/political unity
    Affiliation("Commonwealth Anglo", 4, COMMONWEALTH_ANGLO_DOMINIONS),

    // Rhodesia (self-governing, Anglo settlers) — +4 with GB, +2 with Anglo Dominions
    Affiliation("Empire Rhodesia", 4, "GB" :: EMPIRE_SELF_GOV_ZW),
    Affiliation("Empire Rhodesia Commonwealth", 2, List("CA","AU","NZ","ZA") ++ EMPIRE_SELF_GOV_ZW),

    // Self-governing colonies — +3 with GB, +2 with Anglo Dominions
    Affiliation("Empire Self-Gov", 3, "GB" :: EMPIRE_SELF_GOV),
    Affiliation("Empire Self-Gov Commonwealth", 2, List("CA","AU","NZ","ZA") ++ EMPIRE_SELF_GOV),

    // Crown colonies — +2 with GB, +1 with Anglo Dominions (no inter-colony boost)
    Affiliation("Empire Crown GB", 2, "GB" :: EMPIRE_CROWN_COLONIES),
    Affiliation("Empire Crown Commonwealth", 1, List("CA","AU","NZ","ZA") ++ EMPIRE_CROWN_COLONIES),

    // Domestic markets
    Affiliation("USA Domestic", 5, USA_DOMESTIC),
    Affiliation("GB Domestic",  5, GB_DOMESTIC),
    Affiliation("FR Domestic",  5, FR_DOMESTIC),
    Affiliation("NL Domestic",  5, NL_DOMESTIC),
    Affiliation("AU Domestic",  5, AU_DOMESTIC),
    Affiliation("NZ Domestic",  5, NZ_DOMESTIC),

    // Nauru mandate — +4 with AU, NZ, GB
    Affiliation("Nauru Mandate", 4, NAURU_MANDATE),

    // Papua New Guinea — AU administered +4
    Affiliation("AU PNG", 4, AU_PNG),

    // Western Samoa — NZ mandate +4
    Affiliation("NZ Samoa", 4, NZ_SAMOA),

    // ANZUS — mutual defence +3
    Affiliation("ANZUS", 3, ANZUS),

    // --- Communist bloc ---

    Affiliation("USSR Domestic",   5, USSR_DOMESTIC),
    Affiliation("Yugo Domestic",   5, YUGO_DOMESTIC),
    Affiliation("Czecho Domestic", 5, CZECHO_DOMESTIC),
    Affiliation("Warsaw Pact",     3, WARSAW_PACT),

    // --- NAM ---

    Affiliation("NAM Core", 2, NAM_CORE),

    // French protectorates in MENA — +3 with FR
    Affiliation("France MENA", 3, "FR" :: FR_MENA_PROTECTORATES),

    // French Sub-Saharan Africa — +2 with FR, +1 among themselves (franc zone)
    Affiliation("France Africa GB", 2, "FR" :: FR_AFRICA),
    Affiliation("France Africa Intra", 1, FR_AFRICA),

    // --- existing entries below ---

    // US-Caribbean Relations Boost:
    Affiliation("US Anglo Caribbean", 3, List(
      "US", "CA", "PR", "BM", "AW", "AG", "BB", "BS", "GY", "JM", "KY", "TC", "TT", "VG", "VI"
    )),

    // Denmark Dependants:
    Affiliation("Denmark", 5, List(
      "DK", "GL", "FO"
    )),

    // ANZAC — AU and NZ close relationship, extends to their shared dependencies
    Affiliation("ANZAC common market", 4, List("AU","NZ","CK","NU","WS","PG")),

  )

  // These are per-country mutual relations modifications:
  lazy val FRIENDSHIPS: List[Relation] =
    List(
      // US backing of Baghdad Pact (not a member — TR and GB already covered at +3 by NATO)
      Relation("US", Direction.BI, 1, List("IQ","IR","PK")),

      // Non-Anglo Dominions — sovereign border exists, +2 with GB only
      Relation("GB", Direction.BI, 2, COMMONWEALTH_NON_ANGLO_DOMINIONS),

      // --- Communist bloc bilaterals ---

      Relation("CN", Direction.BI, 2, List("PK")),
      Relation("CN", Direction.BI, 4, List("KP")),
      Relation("RU", Direction.BI, 4, List("KP")),
      Relation("CN", Direction.BI, 3, List("VN")),
      Relation("RU", Direction.BI, 2, List("VN")),

      // --- NAM bilaterals ---

      Relation("IN", Direction.BI, 1, USSR_DOMESTIC),
      Relation("EG", Direction.BI, 1, USSR_DOMESTIC),
      Relation("EG", Direction.BI, 1, List("GB")),
      Relation("EG", Direction.BI, 1, List("US")),

      // France — Levant mandate legacy + core Western allies
      Relation("FR", Direction.BI, 3, List("LB")),
      Relation("FR", Direction.BI, 1, List("SY")),
      Relation("FR", Direction.BI, 4, List("GB","US","CA")),
      Relation("FR", Direction.BI, 2, List("TN","DZ","MA","SN","CI","DJ")),

      // Italy — colonial and Mediterranean ties
      Relation("IT", Direction.BI, 2, List("LY","IL")),

      // Europe
      Relation("CH", Direction.BI, 4, List("FR","DE","AT","IT","ES","NL","BE","DK","SE")),
      Relation("GR", Direction.TO, 3, List("DE","GB","IT","FR")),

      // Portugal colonial ties
      Relation("PT", Direction.BI, 3, List("CV")),

      // Pacific — French/US/AU/NZ administered territories
      Relation("AS", Direction.BI, 4, List("AU","NZ","US","FR")),
      Relation("NC", Direction.BI, 4, List("AU","NZ","AS","US")),
      Relation("NC", Direction.BI, 2, List("FJ")),
      Relation("PF", Direction.BI, 4, List("AU","NZ","CA","US")),
      Relation("FJ", Direction.BI, 2, List("AU","NZ","US","FR")),
      
      // Saint Helena — British territory
      Relation("SH", Direction.BI, 3, List("FK")),

      // E/SE Asia
      Relation("JP", Direction.BI, 2, List("PE","BR","TH")),
      Relation("KR", Direction.BI, 4, List("US")),
      Relation("KR", Direction.BI, 3, List("JP")),
      Relation("TW", Direction.BI, 4, List("US")),
      Relation("PH", Direction.BI, 3, List("US","JP")),
      Relation("TH", Direction.BI, 3, List("JP","US")),
      Relation("ID", Direction.BI, 2, List("IN","NL")),

      // South Asia
      Relation("IN", Direction.BI, 4, List("BT")),
      Relation("IN", Direction.BI, 3, List("NP","LK")),
      Relation("IN", Direction.BI, 2, List("GB","FR","US","CA","MM","AF")),
      Relation("PK", Direction.BI, 2, List("SA","GB","US")),

      // TR — NATO member, regional ties
      Relation("TR", Direction.BI, 4, List("AZ")),
      Relation("TR", Direction.BI, 2, List("IQ","IR","PK","UA","GE")),

      // US key alliances
      Relation("US", Direction.BI, 4, List("JP","KR","TW","AU","NZ")),
      Relation("US", Direction.BI, 2, List("AR","CU","MX")),
      Relation("US", Direction.BI, 1, List("SA","EG")),

      // MENA
      Relation("IL", Direction.BI, 3, List("CA","US")),
      Relation("IL", Direction.BI, 1, List("IN","RO","PL","GB")),
      Relation("SA", Direction.BI, 3, List("PK")),
      Relation("SA", Direction.BI, 2, List("LY","TN","JO","LB")),
      Relation("TN", Direction.BI, 2, List("LY")),
      Relation("EG", Direction.BI, 2, List("LY")),
      Relation("MA", Direction.BI, 3, List("FR","ES","GB","DE","US")),

      // Americas
      Relation("BR", Direction.BI, 3, List("PT","AR","BO","PY","UY","PE","CL")),
      Relation("BR", Direction.BI, 2, List("CO","MX","US","JP","DE")),
      Relation("CO", Direction.BI, 3, List("US","PE","EC","PA","CL")),
      Relation("CO", Direction.BI, 2, List("MX","BR","BO")),
      Relation("PE", Direction.BI, 2, List("CL","BO","EC","CO","MX","US","JP")),
      Relation("CL", Direction.BI, 3, List("PE","AR","US","CA","PA")),
      Relation("CL", Direction.BI, 2, List("MX","JP")),
      Relation("AR", Direction.BI, 2, List("UY","PY")),

      // Sub-Saharan Africa
      Relation("ZA", Direction.BI, 2, List("ZW","GB","US","AU")),
      Relation("ET", Direction.BI, 4, List("DJ")),
      Relation("ET", Direction.BI, 1, List("US","GB")),
  ) ++
    // Yugoslavia bilaterals — all successor codes carry the relation
    YUGO_DOMESTIC.map(c => Relation(c, Direction.BI, 1, USSR_DOMESTIC)) ++
    YUGO_DOMESTIC.map(c => Relation(c, Direction.BI, 2, List("PL","CZ","SK","HU","RO","BG"))) ++
    YUGO_DOMESTIC.map(c => Relation(c, Direction.BI, 1, List("CN"))) ++
    YUGO_DOMESTIC.map(c => Relation(c, Direction.BI, 2, List("US","GB","FR")))

  lazy val ENMITIES: List[Relation] =
    List(
      // --- 1955 Cold War enmities ---

      Relation("KP", Direction.BI, -3, List("KR","US")),
      Relation("CN", Direction.BI, -3, List("KR","US")),
      Relation("CN", Direction.BI, -2, List("JP")),
      Relation("KP", Direction.BI, -2, List("JP")),

      Relation("FR", Direction.BI, -2, List("DZ")),
      Relation("FR", Direction.BI, -1, List("VN")),

      Relation("KP", Direction.BI, -3, List("CA","US","FR","DE","AT","CH","IT","GB","ES","NL","BE","PL","DK","SE","IE","JP","KR","AU","SG")),
      Relation("KP", Direction.BI, -2, List("BN","KH","ID","LA","MY","PH","TH","BR","IN","ZA","TR")),

      Relation("IR", Direction.BI, -3, List("SA")),
      Relation("IL", Direction.BI, -2, List("IQ","YE","LY","SD","KW","SA")),
      Relation("IL", Direction.BI, -3, List("IR","SY","LB")),
      Relation("PK", Direction.BI, -2, List("IN")),
      Relation("TR", Direction.BI, -1, List("GR","CY")),
  ) ++
    // Yugoslavia-Albania tension
    YUGO_DOMESTIC.map(c => Relation(c, Direction.BI, -1, List("AL")))


  mainFlow()



  def mainFlow() = {
    var mutualRelationshipMap = getCountryMutualRelationship()
//    val mutualRelationshipPatchMap = getCountryMutualRelationshipPatch()

    mutualRelationshipMap = affiliationAdjustment(mutualRelationshipMap)
    mutualRelationshipMap = relationAdjustment(mutualRelationshipMap, FRIENDSHIPS)
    mutualRelationshipMap = relationAdjustment(mutualRelationshipMap, ENMITIES)

    println("Saving country mutual relationships: " + mutualRelationshipMap)

    CountrySource.updateCountryMutualRelationships(mutualRelationshipMap)

    println("DONE")
  }

  def affiliationAdjustment(existingMap : mutable.Map[(String, String), Int]) : Map[(String, String), Int] = {
    println(s"affiliations: $AFFILIATIONS")
    AFFILIATIONS.foreach {
      case Affiliation(id, relationship, members) =>
        members.foreach { memberX =>
          if (CountrySource.loadCountryByCode(memberX).isDefined) {
            members.foreach { memberY =>
              if (memberX != memberY) {
                val shouldPatch = existingMap.get((memberX, memberY)) match {
                  case Some(existingValue) => existingValue < relationship
                  case None => true
                }
                if (shouldPatch) {
                  println(s"patching $memberX vs $memberY from $id with $relationship")
                  existingMap.put((memberX, memberY), relationship)
                } else {
                  println(s"Not patching $memberX vs $memberY from $id with $relationship as existing value is greater")
                }
              }
            }
          } else {
            println(s"Country code $memberX not found")
          }
        }
    }
    existingMap
  }

  def relationAdjustment(existingMap: mutable.Map[(String, String), Int], adjustmentMap: List[Relation] ): Map[(String, String), Int] = {
    import Direction._
    adjustmentMap.foreach {
      case Relation(id, direction, relationship, members) =>
        members.foreach { member =>
          if (CountrySource.loadCountryByCode(member).isDefined && member != id) {
            if(direction == Direction.TO){
              existingMap.put((member, id), relationship)
              println(s"$member -> $id with $relationship")
            } else if (direction == Direction.FROM) {
              existingMap.put((id, member), relationship)
              println(s"$id -> $member with $relationship")
            } else {
              existingMap.put((id, member), relationship)
              existingMap.put((member, id), relationship)
              println(s"$id <-> $member with $relationship")
            }
          } else {
            println(s"Country code $member not found | duplicate entry")
          }
        }
    }
    existingMap
  }

  /**
   * get from country-mutual-relationship.csv
   */
  def getCountryMutualRelationship() = {
    val nameToCode = CountrySource.loadAllCountries().map( country => (country.name, country.countryCode)).toMap
//    val linesIter = scala.io.Source.fromFile("country-mutual-relationship.csv").getLines()
//    val headerLine = linesIter.next()
//
//    val countryHeader = headerLine.split(',').filter(!_.isEmpty())
//
    val mutualRelationshipMap = Map[(String, String), Int]()
//
//    while (linesIter.hasNext) {
//      val tokens = linesIter.next().split(',').filter(!_.isEmpty())
//      //first token is the country name itself
//      val fromCountry = tokens(0)
//      for (i <- 1 until tokens.size) {
//        val relationship = tokens(i)
//        val strength = relationship.count( _ == '1') //just count the number of ones should be sufficient
//        val toCountry = countryHeader(i - 1)
//        //println(fromCountry + " " + toCountry + " " + strength)
//        if (strength > 0) {
//          if (nameToCode.contains(fromCountry) && nameToCode.contains(toCountry)) {
//            mutualRelationshipMap.put((nameToCode(fromCountry), nameToCode(toCountry)), strength)
//          }
//        }
//      }
//    }

    nameToCode.values.foreach { countryCode =>
      mutualRelationshipMap.put((countryCode, countryCode), 5) //country with itself is 5 HomeCountry
    }

    mutualRelationshipMap
  }

  /**
   * patch from country-mutual-relationship-patch.csv
   */
  def getCountryMutualRelationshipPatch() = {
    val linesIter = scala.io.Source.fromFile("country-mutual-relationship-patch.csv").getLines()
    val mutualRelationshipMap = Map[(String, String), Int]()
    
    while (linesIter.hasNext) {
      val tokens = linesIter.next().split(',')
      //first token is the country name itself
      val fromCountry = tokens(0)
      val toCountry = tokens(1)
      val strength = Integer.valueOf(tokens(2))
      mutualRelationshipMap.put((fromCountry, toCountry), strength)
      mutualRelationshipMap.put((toCountry, fromCountry), strength)
    }
    mutualRelationshipMap
  }
  case class Relation(id : String, direction : Direction.Value, relationship: Int, members : List[String])

  object Direction extends Enumeration {
    type Direction = Value
    val FROM, TO, BI = Value
  }

  case class Affiliation(id : String, relationship: Int, members : List[String])



}