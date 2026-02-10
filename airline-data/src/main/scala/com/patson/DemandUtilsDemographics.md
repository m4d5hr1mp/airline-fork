// DemandUtilsDemographics.scala
package com.patson

import scala.collection.immutable.ListMap // For ordered maps if needed; otherwise, use standard Map

object DemandUtilsDemographics {
  // Country-Specific Datapoints
  case class CountryDemographics(
    languages: Seq[String],      // Language groups
    cultures: Seq[String],       // Cultural/regional identities
    geopoliticalBlocs: Seq[String],  // Economic/political unions
    preferredVacationDestinations: Seq[String]
  )

  // Languages
  val referenceLanguages: Set[String] = Set(
    "Anglophone", "Francophone", "Hispanic", "Lusophone", 
    "Arab", "Sinophone", "Malay", "Hindi", "Dutch", "Turkic", 
    "Germanic", "Swahili", "Bengali", "Punjabi", "Khmer"
  )
  // Cultures (regional/ethnic/religious identities)
  val referenceCultures: Set[String] = Set(
    "Sunni", "Shia", "Buddhist", "Hindu", "Jewish",
    "Nordic", "Baltics", "ANZAC", "Commonwealth",
    "Russian", "CIS", "Armenian", "Tajik", "Berber",
    "Dutch Caribbean",
  )
  // Geopolitical Blocs (economic/political unions)
  val referenceGeopoliticalBlocs: Set[String] = Set(
    "EU", "OECD", "NAFTA", "APEC", "ASEAN", "MERCOSUR", 
    "OPEC", "BRICS", "ECOWAS", "COMESA", "Belt & Road", "Gulf Council"
  )

  // Map of ISO2 country codes to their additional demand modeling data:
  val countryDemographics: Map[String, CountryDemographics] = ListMap(

    "AE" -> CountryDemographics(// Emirates
      languages = Seq("Anglophone", "Arab"),
      cultures = Seq("Sunni","Arab"),
      geopoliticalBlocs = Seq("BRICS", "OPEC", "Gulf Council"),
      preferredVacationDestinations = Seq("TH", "MV", "SC", "ID")
    ),
    "AF" -> CountryDemographics(// Afghanistan
      languages = Seq("Pashtu"),
      cultures = Seq("Sunni"),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "AG" -> CountryDemographics(// Antigua and Barbuda
      languages = Seq("Anglophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "AI" -> CountryDemographics(// Anguilla
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "AL" -> CountryDemographics(// Albania
      languages = Seq(),
      cultures = Seq("Sunni"),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "AM" -> CountryDemographics(// Armenia
      languages = Seq("Russian","Armenian"),
      cultures = Seq(),
      geopoliticalBlocs = Seq("CIS"),
      preferredVacationDestinations = Seq()
    ),
    "AO" -> CountryDemographics(// Angola
      languages = Seq("Lusophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "AR" -> CountryDemographics(// Argentina
      languages = Seq("Hispanic"),
      cultures = Seq(),
      geopoliticalBlocs = Seq("MERCOSUR","Belt & Road"),
      preferredVacationDestinations = Seq("BR","US","UY","CL")
    ),
    "AS" -> CountryDemographics(// American Samoa
      languages = Seq("Anglophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "AT" -> CountryDemographics(// Austria
      languages = Seq("Germanic"),
      cultures = Seq(),
      geopoliticalBlocs = Seq("EU","OECD"),
      preferredVacationDestinations = Seq("ES","IT","HR","GR")
    ),
    "AU" -> CountryDemographics(// Australia
      languages = Seq("Anglophone"),
      cultures = Seq("Commonwealth","ANZAC"),
      geopoliticalBlocs = Seq("APEC","OECD"),
      preferredVacationDestinations = Seq("ID","FJ","TH","US")
    ),
    "AW" -> CountryDemographics(// Aruba
      languages = Seq("Dutch","Papiamento"),
      cultures = Seq("Dutch Caribbean"),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "AZ" -> CountryDemographics(// Azerbaijan
      languages = Seq(),
      cultures = Seq("Turkic","Shia"),
      geopoliticalBlocs = Seq("CIS","OPEC","Belt & Road"),
      preferredVacationDestinations = Seq()
    ),
    "BA" -> CountryDemographics(// Bosnia and Herzegovina
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "BB" -> CountryDemographics(// Barbados
      languages = Seq("Anglophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "BD" -> CountryDemographics(// Bangladesh
      languages = Seq("Anglophone"),
      cultures = Seq("Sunni"),
      geopoliticalBlocs = Seq("Belt & Road"),
      preferredVacationDestinations = Seq()
    ),
    "BE" -> CountryDemographics(// Belgium
      languages = Seq("Francophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq("EU","OECD"),
      preferredVacationDestinations = Seq("ES","FR","IT","TR")
    ),
    "BF" -> CountryDemographics(// Burkina Faso
      languages = Seq("Francophone"),
      cultures = Seq("Sunni"),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "BG" -> CountryDemographics(// Bulgaria
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq("EU"),
      preferredVacationDestinations = Seq()
    ),
    "BH" -> CountryDemographics(// Bahrain
      languages = Seq("Arab"),
      cultures = Seq("Shia","Sunni"),
      geopoliticalBlocs = Seq("OPEC","Gulf Council"),
      preferredVacationDestinations = Seq("TH","MV","SC","ID")
    ),
    "BI" -> CountryDemographics(// Burundi
      languages = Seq("Anglophone","Francophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq("COMESA"),
      preferredVacationDestinations = Seq()
    ),
    "BJ" -> CountryDemographics(// Benin
      languages = Seq("Francophone"),
      cultures = Seq("Sunni"),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "BL" -> CountryDemographics(// Saint Barthélemy
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "BM" -> CountryDemographics(// Bermuda
      languages = Seq("Anglophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq("OECD"),
      preferredVacationDestinations = Seq()
    ),
    "BN" -> CountryDemographics(// Brunei Darussalam
      languages = Seq("Anglophone"),
      cultures = Seq("Malay","Sunni"),
      geopoliticalBlocs = Seq("OPEC"),
      preferredVacationDestinations = Seq()
    ),
    "BO" -> CountryDemographics(// Bolivia
      languages = Seq("Hispanic"),
      cultures = Seq(),
      geopoliticalBlocs = Seq("MERCOSUR","Belt & Road"),
      preferredVacationDestinations = Seq()
    ),
    "BQ" -> CountryDemographics(// Bonaire
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "BR" -> CountryDemographics(// Brazil
      languages = Seq("Lusophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq("BRICS","MERCOSUR"),
      preferredVacationDestinations = Seq("US","AR","PT","FR")
    ),
    "BS" -> CountryDemographics(// Bahamas, The
      languages = Seq("Anglophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "BT" -> CountryDemographics(// Bhutan
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "BV" -> CountryDemographics(// Bouvet Island
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "BW" -> CountryDemographics(// Botswana
      languages = Seq("Anglophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "BY" -> CountryDemographics(// Belarus
      languages = Seq("Russian"),
      cultures = Seq(),
      geopoliticalBlocs = Seq("CIS"),
      preferredVacationDestinations = Seq("TR","EG")
    ),
    "BZ" -> CountryDemographics(// Belize
      languages = Seq("Anglophone","Hispanic"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "CA" -> CountryDemographics(// Canada
      languages = Seq("Anglophone"),
      cultures = Seq("Commonwealth"),
      geopoliticalBlocs = Seq("NAFTA","OECD"),
      preferredVacationDestinations = Seq("US","CU","MX","BS","DO","CR")
    ),
    "CC" -> CountryDemographics(// Cocos (Keeling) Islands
      languages = Seq("Anglophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "CD" -> CountryDemographics(// Congo (Democratic Republic of the)
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "CF" -> CountryDemographics(// Central African Republic
      languages = Seq("Francophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "CG" -> CountryDemographics(// Congo, Rep.
      languages = Seq("Francophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq("OPEC"),
      preferredVacationDestinations = Seq()
    ),
    "CH" -> CountryDemographics(// Switzerland
      languages = Seq("Anglophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq("OECD"),
      preferredVacationDestinations = Seq("ES","TR","EG","GR")
    ),
    "CI" -> CountryDemographics(// Cote d'Ivoire
      languages = Seq("Francophone"),
      cultures = Seq("Sunni"),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "CK" -> CountryDemographics(// Cook Islands
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "CL" -> CountryDemographics(// Chile
      languages = Seq("Hispanic"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq("AR","BR")
    ),
    "CM" -> CountryDemographics(// Cameroon
      languages = Seq("Anglophone","Francophone"),
      cultures = Seq("Sunni"),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "CN" -> CountryDemographics(// China
      languages = Seq("Sinophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq("BRICS","Belt & Road"),
      preferredVacationDestinations = Seq("TH","VN","MV")
    ),
    "CO" -> CountryDemographics(// Colombia
      languages = Seq("Hispanic"),
      cultures = Seq(),
      geopoliticalBlocs = Seq("Belt & Road"),
      preferredVacationDestinations = Seq()
    ),
    "CR" -> CountryDemographics(// Costa Rica
      languages = Seq("Hispanic","Anglophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "CU" -> CountryDemographics(// Cuba
      languages = Seq("Hispanic"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "CV" -> CountryDemographics(// Cabo Verde
      languages = Seq("Lusophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "CW" -> CountryDemographics(// Curacao
      languages = Seq("Dutch","Papiamento"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "CX" -> CountryDemographics(// Christmas Island
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "CY" -> CountryDemographics(// Cyprus
      languages = Seq("Anglophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq("EU"),
      preferredVacationDestinations = Seq()
    ),
    "CZ" -> CountryDemographics(// Czechia
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq("EU"),
      preferredVacationDestinations = Seq()
    ),
    "DE" -> CountryDemographics(// Germany
      languages = Seq("German"),
      cultures = Seq(),
      geopoliticalBlocs = Seq("OECD","EU"),
      preferredVacationDestinations = Seq("ES","GR","TR","EG")
    ),
    "DJ" -> CountryDemographics(// Djibouti
      languages = Seq("Francophone","Arab"),
      cultures = Seq("Sunni"),
      geopoliticalBlocs = Seq("Belt & Road"),
      preferredVacationDestinations = Seq()
    ),
    "DK" -> CountryDemographics(// Denmark
      languages = Seq(),
      cultures = Seq("Nordic"),
      geopoliticalBlocs = Seq("OECD","EU"),
      preferredVacationDestinations = Seq("ES","GR","IT","TH")
    ),
    "DM" -> CountryDemographics(// Dominica
      languages = Seq("Anglophone","Francophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "DO" -> CountryDemographics(// Dominican Republic
      languages = Seq("Hispanic"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "DZ" -> CountryDemographics(// Algeria
      languages = Seq("Francophone","Arab","Berber"),
      cultures = Seq("Sunni"),
      geopoliticalBlocs = Seq("OPEC"),
      preferredVacationDestinations = Seq()
    ),
    "EC" -> CountryDemographics(// Ecuador
      languages = Seq("Hispanic"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "EE" -> CountryDemographics(// Estonia
      languages = Seq(),
      cultures = Seq("Baltics"),
      geopoliticalBlocs = Seq("EU"),
      preferredVacationDestinations = Seq()
    ),
    "EG" -> CountryDemographics(// Egypt, Arab Rep.
      languages = Seq("Arab","Anglophone"),
      cultures = Seq("Sunni"),
      geopoliticalBlocs = Seq("BRICS","Belt & Road"),
      preferredVacationDestinations = Seq()
    ),
    "EH" -> CountryDemographics(// Western Sahara
      languages = Seq("Arab"),
      cultures = Seq("Sunni"),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "ER" -> CountryDemographics(// Eritrea
      languages = Seq("Arab"),
      cultures = Seq("Sunni"),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "ES" -> CountryDemographics(// Spain
      languages = Seq("Hispanic"),
      cultures = Seq(),
      geopoliticalBlocs = Seq("OECD","EU"),
      preferredVacationDestinations = Seq()
    ),
    "ET" -> CountryDemographics(// Ethiopia
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq("BRICS","Belt & Road"),
      preferredVacationDestinations = Seq()
    ),
    "FI" -> CountryDemographics(// Finland
      languages = Seq("Anglophone"),
      cultures = Seq("Nordic"),
      geopoliticalBlocs = Seq("OECD","EU"),
      preferredVacationDestinations = Seq("ES","GR","IT","TH")
    ),
    "FJ" -> CountryDemographics(// Fiji
      languages = Seq("Anglophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "FK" -> CountryDemographics(// Falkland Islands
      languages = Seq("Anglophone"),
      cultures = Seq("Commonwealth"),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "FM" -> CountryDemographics(// Micronesia, Fed. Sts.
      languages = Seq("Anglophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "FO" -> CountryDemographics(// Faroe Islands
      languages = Seq(),
      cultures = Seq("Nodric"),
      geopoliticalBlocs = Seq("EU"),
      preferredVacationDestinations = Seq()
    ),
    "FR" -> CountryDemographics(// France
      languages = Seq("Francophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq("OECD","EU"),
      preferredVacationDestinations = Seq()
    ),
    "GA" -> CountryDemographics(// Gabon
      languages = Seq("Francophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq("OPEC"),
      preferredVacationDestinations = Seq()
    ),
    "GB" -> CountryDemographics(// United Kingdom
      languages = Seq("Anglophone"),
      cultures = Seq("Commonwealth"),
      geopoliticalBlocs = Seq("OECD"),
      preferredVacationDestinations = Seq("ES","PT","MA","GR")
    ),
    "GD" -> CountryDemographics(// Grenada
      languages = Seq("Francophone","Anglophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "GE" -> CountryDemographics(// Georgia
      languages = Seq("Russian"),
      cultures = Seq(),
      geopoliticalBlocs = Seq("CIS"),
      preferredVacationDestinations = Seq()
    ),
    "GF" -> CountryDemographics(// French Guiana
      languages = Seq("Francophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq("EU"),
      preferredVacationDestinations = Seq()
    ),
    "GG" -> CountryDemographics(// Guernsey
      languages = Seq("Anglophone"),
      cultures = Seq("Commonwealth"),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "GH" -> CountryDemographics(// Ghana
      languages = Seq("Anglophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "GI" -> CountryDemographics(// Gibraltar
      languages = Seq("Anglophone"),
      cultures = Seq("Commonwealth"),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "GL" -> CountryDemographics(// Greenland
      languages = Seq(),
      cultures = Seq("Nordic"),
      geopoliticalBlocs = Seq("EU"),
      preferredVacationDestinations = Seq()
    ),
    "GM" -> CountryDemographics(// Gambia, The
      languages = Seq("Anglophone"),
      cultures = Seq("Sunni"),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "GN" -> CountryDemographics(// Guinea
      languages = Seq("Francophone"),
      cultures = Seq("Sunni"),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "GP" -> CountryDemographics(// Guadeloupe
      languages = Seq("Francophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "GQ" -> CountryDemographics(// Equatorial Guinea
      languages = Seq("Hispanic","Lusophone","Francophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq("OPEC"),
      preferredVacationDestinations = Seq()
    ),
    "GR" -> CountryDemographics(// Greece
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq("OECD","EU"),
      preferredVacationDestinations = Seq()
    ),
    "GS" -> CountryDemographics(// South Georgia and the South Sandwich Island
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "GT" -> CountryDemographics(// Guatemala
      languages = Seq("Hispanic"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "GU" -> CountryDemographics(// Guam
      languages = Seq("Anglophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "GW" -> CountryDemographics(// Guinea-Bissau
      languages = Seq("Lusophone"),
      cultures = Seq("Sunni"),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "GY" -> CountryDemographics(// Guyana
      languages = Seq("Anglophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "HK" -> CountryDemographics(// Hong Kong SAR, China
      languages = Seq("Anglophone","Sinophone"),
      cultures = Seq("Commonwealth"),
      geopoliticalBlocs = Seq("OECD","ASEAN","APEC"),
      preferredVacationDestinations = Seq("JP","TH","KR","SN")
    ),
    "HM" -> CountryDemographics(// Heard Island and McDonald Islands
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "HN" -> CountryDemographics(// Honduras
      languages = Seq("Hispanic"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "HR" -> CountryDemographics(// Croatia
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq("EU"),
      preferredVacationDestinations = Seq()
    ),
    "HT" -> CountryDemographics(// Haiti
      languages = Seq("Francophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "HU" -> CountryDemographics(// Hungary
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq("EU"),
      preferredVacationDestinations = Seq()
    ),
    "ID" -> CountryDemographics(// Indonesia
      languages = Seq(),
      cultures = Seq("Sunni"),
      geopoliticalBlocs = Seq("BRICS","Belt & Road"),
      preferredVacationDestinations = Seq("MY","SN","TH","JP")
    ),
    "IE" -> CountryDemographics(// Ireland
      languages = Seq("Anglophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq("OECD","EU"),
      preferredVacationDestinations = Seq("ES","PT","IT","US")
    ),
    "IL" -> CountryDemographics(// Israel
      languages = Seq("Anglophone"),
      cultures = Seq("Jewish"),
      geopoliticalBlocs = Seq("OECD"),
      preferredVacationDestinations = Seq()
    ),
    "IM" -> CountryDemographics(// Isle of Man
      languages = Seq("Anglophone"),
      cultures = Seq("Commonwealth"),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "IN" -> CountryDemographics(// India
      languages = Seq("Anglophone","Hindi"),
      cultures = Seq(),
      geopoliticalBlocs = Seq("BRICS"),
      preferredVacationDestinations = Seq("TH","MV","ID")
    ),
    "IO" -> CountryDemographics(// British Indian Ocean Territory
      languages = Seq("Anglophone"),
      cultures = Seq("Commonwealth"),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "IQ" -> CountryDemographics(// Iraq
      languages = Seq("Arab","Kurdish"),
      cultures = Seq("Shia","Sunni"),
      geopoliticalBlocs = Seq("OPEC","Belt & Road"),
      preferredVacationDestinations = Seq()
    ),
    "IR" -> CountryDemographics(// Iran, Islamic Rep.
      languages = Seq("Farsi"),
      cultures = Seq("Shia"),
      geopoliticalBlocs = Seq("BRICS","OPEC"),
      preferredVacationDestinations = Seq()
    ),
    "IS" -> CountryDemographics(// Iceland
      languages = Seq("Anglophone"),
      cultures = Seq("Nordic"),
      geopoliticalBlocs = Seq("OECD"),
      preferredVacationDestinations = Seq()
    ),
    "IT" -> CountryDemographics(// Italy
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq("OECD","EU"),
      preferredVacationDestinations = Seq()
    ),
    "JE" -> CountryDemographics(// Jersey
      languages = Seq("Anglophone"),
      cultures = Seq("Commonwealth"),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "JM" -> CountryDemographics(// Jamaica
      languages = Seq("Anglophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "JO" -> CountryDemographics(// Jordan
      languages = Seq("Arab"),
      cultures = Seq("Sunni"),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "JP" -> CountryDemographics(// Japan
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq("APEC","OECD"),
      preferredVacationDestinations = Seq("KR","TH","VN","US")
    ),
    "KE" -> CountryDemographics(// Kenya
      languages = Seq("Anglophone","Swahili"),
      cultures = Seq(),
      geopoliticalBlocs = Seq("COMESA","Belt & Road"),
      preferredVacationDestinations = Seq()
    ),
    "KG" -> CountryDemographics(// Kyrgyz Republic
      languages = Seq("Russian"),
      cultures = Seq("Turkic"),
      geopoliticalBlocs = Seq("CIS"),
      preferredVacationDestinations = Seq()
    ),
    "KH" -> CountryDemographics(// Cambodia
      languages = Seq("Khmer"),
      cultures = Seq("Buddhist"),
      geopoliticalBlocs = Seq("Belt & Road"),
      preferredVacationDestinations = Seq()
    ),
    "KI" -> CountryDemographics(// Kiribati
      languages = Seq("Anglophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "KM" -> CountryDemographics(// Comoros
      languages = Seq("Francophone","Arab"),
      cultures = Seq("Sunni"),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "KN" -> CountryDemographics(// St. Kitts and Nevis
      languages = Seq("Anglophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "KP" -> CountryDemographics(// Korea, Dem. People's Rep.
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "KR" -> CountryDemographics(// Korea, Rep.
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq("OECD","APEC"),
      preferredVacationDestinations = Seq("JP","VN","TH","ID")
    ),
    "KW" -> CountryDemographics(// Kuwait
      languages = Seq("Arab"),
      cultures = Seq("Sunni","Shia"),
      geopoliticalBlocs = Seq("OPEC","Gulf Council"),
      preferredVacationDestinations = Seq("TH","MV","SC","ID")
    ),
    "KY" -> CountryDemographics(// Cayman Islands
      languages = Seq("Anglophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "KZ" -> CountryDemographics(// Kazakhstan
      languages = Seq("Russian"),
      cultures = Seq("Turkic"),
      geopoliticalBlocs = Seq("CIS","Belt & Road"),
      preferredVacationDestinations = Seq()
    ),
    "LA" -> CountryDemographics(// Lao PDR
      languages = Seq(),
      cultures = Seq("Buddhist"),
      geopoliticalBlocs = Seq("ASEAN","Belt & Road"),
      preferredVacationDestinations = Seq()
    ),
    "LB" -> CountryDemographics(// Lebanon
      languages = Seq("Francophone","Arab"),
      cultures = Seq("Sunni","Shia"),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "LC" -> CountryDemographics(// St. Lucia
      languages = Seq("Anglophone","Francophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "LI" -> CountryDemographics(// Liechtenstein
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq("EU","OECD"),
      preferredVacationDestinations = Seq()
    ),
    "LK" -> CountryDemographics(// Sri Lanka
      languages = Seq("Anglophone","Tamil"),
      cultures = Seq("Bhuddist"),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "LR" -> CountryDemographics(// Liberia
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "LS" -> CountryDemographics(// Lesotho
      languages = Seq("Anglophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "LT" -> CountryDemographics(// Lithuania
      languages = Seq(),
      cultures = Seq("Baltics"),
      geopoliticalBlocs = Seq("EU"),
      preferredVacationDestinations = Seq()
    ),
    "LU" -> CountryDemographics(// Luxembourg
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq("EU"),
      preferredVacationDestinations = Seq()
    ),
    "LV" -> CountryDemographics(// Latvia
      languages = Seq(),
      cultures = Seq("Baltics"),
      geopoliticalBlocs = Seq("EU"),
      preferredVacationDestinations = Seq()
    ),
    "LY" -> CountryDemographics(// Libya
      languages = Seq("Arab"),
      cultures = Seq("Sunni"),
      geopoliticalBlocs = Seq("OPEC"),
      preferredVacationDestinations = Seq()
    ),
    "MA" -> CountryDemographics(// Morocco
      languages = Seq("Francophone","Arab","Berber"),
      cultures = Seq("Sunni"),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "MD" -> CountryDemographics(// Moldova
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "ME" -> CountryDemographics(// Montenegro
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "MF" -> CountryDemographics(// St. Martin (French part)
      languages = Seq("Francophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq("EU"),
      preferredVacationDestinations = Seq()
    ),
    "MG" -> CountryDemographics(// Madagascar
      languages = Seq("Francophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "MH" -> CountryDemographics(// Marshall Islands
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "MK" -> CountryDemographics(// North Macedonia
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "ML" -> CountryDemographics(// Mali
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "MM" -> CountryDemographics(// Myanmar
      languages = Seq("Anglophone"),
      cultures = Seq("Bhuddist"),
      geopoliticalBlocs = Seq("ASEAN"),
      preferredVacationDestinations = Seq()
    ),
    "MN" -> CountryDemographics(// Mongolia
      languages = Seq(),
      cultures = Seq("Turkic"),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "MO" -> CountryDemographics(// Macao SAR, China
      languages = Seq("Lusophone","Sinophone","Anglophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq("JP","TH","KR","MV","ID")
    ),
    "MP" -> CountryDemographics(// Northern Mariana Islands
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "MQ" -> CountryDemographics(// Martinique
      languages = Seq("Francophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "MR" -> CountryDemographics(// Mauritania
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "MS" -> CountryDemographics(// Montserrat
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "MT" -> CountryDemographics(// Malta
      languages = Seq("Anglophone"),
      cultures = Seq("Commonwealth"),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "MU" -> CountryDemographics(// Mauritius
      languages = Seq("Anglophone","Francophone"),
      cultures = Seq("Hindu"),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "MV" -> CountryDemographics(// Maldives
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "MW" -> CountryDemographics(// Malawi
      languages = Seq("Anglophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "MX" -> CountryDemographics(// Mexico
      languages = Seq("Hispanic"),
      cultures = Seq(),
      geopoliticalBlocs = Seq("NAFTA","OECD"),
      preferredVacationDestinations = Seq("US","CA")
    ),
    "MY" -> CountryDemographics(// Malaysia
      languages = Seq("Anglophone","Malay","Tamil"),
      cultures = Seq("Sunni"),
      geopoliticalBlocs = Seq("ASEAN","OPEC"),
      preferredVacationDestinations = Seq("TH","ID","SN","VN")
    ),
    "MZ" -> CountryDemographics(// Mozambique
      languages = Seq("Lusophone"),
      cultures = Seq("Sunni"),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "NA" -> CountryDemographics(// Namibia
      languages = Seq("Anglophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "NC" -> CountryDemographics(// New Caledonia
      languages = Seq("Francophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "NE" -> CountryDemographics(// Niger
      languages = Seq("Francophone"),
      cultures = Seq("Sunni"),
      geopoliticalBlocs = Seq("Belt & Road"),
      preferredVacationDestinations = Seq()
    ),
    "NF" -> CountryDemographics(// Norfolk Island
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "NG" -> CountryDemographics(// Nigeria
      languages = Seq("Anglophone"),
      cultures = Seq("Sunni"),
      geopoliticalBlocs = Seq("OPEC"),
      preferredVacationDestinations = Seq()
    ),
    "NI" -> CountryDemographics(// Nicaragua
      languages = Seq("Hispanic"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "NL" -> CountryDemographics(// Netherlands
      languages = Seq("Dutch","Anglophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq("OECD","EU"),
      preferredVacationDestinations = Seq("ES","PT","ID","TH")
    ),
    "NO" -> CountryDemographics(// Norway
      languages = Seq(),
      cultures = Seq("Nordic"),
      geopoliticalBlocs = Seq("OECD","EU"),
      preferredVacationDestinations = Seq("ES","GR","TH")
    ),
    "NP" -> CountryDemographics(// Nepal
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "NR" -> CountryDemographics(// Nauru
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "NU" -> CountryDemographics(// Niue
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "NZ" -> CountryDemographics(// New Zealand
      languages = Seq("Anglophone"),
      cultures = Seq("Commonwealth","ANZAC"),
      geopoliticalBlocs = Seq("OECD","APEC"),
      preferredVacationDestinations = Seq("FJ","CK","US")
    ),
    "OM" -> CountryDemographics(// Oman
      languages = Seq("Arab"),
      cultures = Seq("Sunni"),
      geopoliticalBlocs = Seq("OPEC","Gulf Council"),
      preferredVacationDestinations = Seq("TH","MV","SC","ID")
    ),
    "PA" -> CountryDemographics(// Panama
      languages = Seq("Hispanic"),
      cultures = Seq(),
      geopoliticalBlocs = Seq("OECD","Belt & Road"),
      preferredVacationDestinations = Seq()
    ),
    "PE" -> CountryDemographics(// Peru
      languages = Seq("Hispanic"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "PF" -> CountryDemographics(// French Polynesia
      languages = Seq("Francophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "PG" -> CountryDemographics(// Papua New Guinea
      languages = Seq("Anglophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "PH" -> CountryDemographics(// Philippines
      languages = Seq("Hispanic"),
      cultures = Seq(),
      geopoliticalBlocs = Seq("ASEAN"),
      preferredVacationDestinations = Seq()
    ),
    "PK" -> CountryDemographics(// Pakistan
      languages = Seq("Anglophone","Punjabi"),
      cultures = Seq("Sunni"),
      geopoliticalBlocs = Seq("Belt & Road"),
      preferredVacationDestinations = Seq()
    ),
    "PL" -> CountryDemographics(// Poland
      languages = Seq(),
      cultures = Seq("Baltics"),
      geopoliticalBlocs = Seq("OECD","EU"),
      preferredVacationDestinations = Seq("TR","GR","ES","EG")
    ),
    "PM" -> CountryDemographics(// Saint Pierre and Miquelon
      languages = Seq("Francophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq("EU"),
      preferredVacationDestinations = Seq()
    ),
    "PN" -> CountryDemographics(// Pitcairn
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "PR" -> CountryDemographics(// Puerto Rico (US)
      languages = Seq("Anglophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq("NAFTA"),
      preferredVacationDestinations = Seq()
    ),
    "PT" -> CountryDemographics(// Portugal
      languages = Seq("Lusophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq("OECD","EU"),
      preferredVacationDestinations = Seq()
    ),
    "PW" -> CountryDemographics(// Palau
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "PY" -> CountryDemographics(// Paraguay
      languages = Seq("Hispanic"),
      cultures = Seq(),
      geopoliticalBlocs = Seq("MERCOSUR"),
      preferredVacationDestinations = Seq()
    ),
    "QA" -> CountryDemographics(// Qatar
      languages = Seq("Arab"),
      cultures = Seq("Sunni"),
      geopoliticalBlocs = Seq("Gulf Council"),
      preferredVacationDestinations = Seq("TH","MV","SC","ID")
    ),
    "RE" -> CountryDemographics(// Réunion
      languages = Seq("Francophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq("EU"),
      preferredVacationDestinations = Seq()
    ),
    "RO" -> CountryDemographics(// Romania
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq("EU"),
      preferredVacationDestinations = Seq()
    ),
    "RS" -> CountryDemographics(// Serbia
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq("Belt & Road"),
      preferredVacationDestinations = Seq("ES","TR","IT","HR")
    ),
    "RU" -> CountryDemographics(// Russian Federation
      languages = Seq("Russiam"),
      cultures = Seq(),
      geopoliticalBlocs = Seq("CIS","BRICS","OPEC"),
      preferredVacationDestinations = Seq("TR","EG","TH")
    ),
    "RW" -> CountryDemographics(// Rwanda
      languages = Seq("Anglophone","Francophone","Swahili"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq("COMESA")
    ),
    "SA" -> CountryDemographics(// Saudi Arabia
      languages = Seq("Arab"),
      cultures = Seq("Sunni"),
      geopoliticalBlocs = Seq("OPEC","Gulf Council"),
      preferredVacationDestinations = Seq("TH","MV","SC","ID")
    ),
    "SB" -> CountryDemographics(// Solomon Islands
      languages = Seq("Anglophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "SC" -> CountryDemographics(// Seychelles
      languages = Seq("Francophone","Anglophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "SD" -> CountryDemographics(// Sudan
      languages = Seq("Arab"),
      cultures = Seq("Sunni"),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "SE" -> CountryDemographics(// Sweden
      languages = Seq("Anglophone"),
      cultures = Seq("Nordic"),
      geopoliticalBlocs = Seq("OECD","EU"),
      preferredVacationDestinations = Seq("ES","PT","GR","IT")
    ),
    "SG" -> CountryDemographics(// Singapore
      languages = Seq("Anglophone","Malay","Sinophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq("ASEAN"),
      preferredVacationDestinations = Seq("TH","ID","KR","JP")
    ),
    "SH" -> CountryDemographics(// Saint Helena
      languages = Seq("Anglophone"),
      cultures = Seq("Commonwealth"),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "SI" -> CountryDemographics(// Slovenia
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq("EU"),
      preferredVacationDestinations = Seq("HR","IT","GR","TR")
    ),
    "SJ" -> CountryDemographics(// Svalbard and Jan Mayen
      languages = Seq(),
      cultures = Seq("Nordic"),
      geopoliticalBlocs = Seq("EU"),
      preferredVacationDestinations = Seq()
    ),
    "SK" -> CountryDemographics(// Slovak Republic
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq("EU"),
      preferredVacationDestinations = Seq("HR","IT","GR","TR")
    ),
    "SL" -> CountryDemographics(// Sierra Leone
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "SN" -> CountryDemographics(// Senegal
      languages = Seq("Francophone"),
      cultures = Seq("Sunni"),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "SO" -> CountryDemographics(// Somalia, Fed. Rep.
      languages = Seq("Arab"),
      cultures = Seq("Sunni"),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "SR" -> CountryDemographics(// Suriname
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "SS" -> CountryDemographics(// South Sudan
      languages = Seq("Anglophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "ST" -> CountryDemographics(// Sao Tome and Principe
      languages = Seq("Lusophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "SV" -> CountryDemographics(// El Salvador
      languages = Seq("Hispanic"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "SX" -> CountryDemographics(// Sint Maarten (Dutch part)
      languages = Seq("Dutch","Anglophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq("EU"),
      preferredVacationDestinations = Seq()
    ),
    "SY" -> CountryDemographics(// Syrian Arab Republic
      languages = Seq("Arab"),
      cultures = Seq("Sunni","Shia"),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "SZ" -> CountryDemographics(// Eswatini
      languages = Seq("Anglophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "TC" -> CountryDemographics(// Turks and Caicos Islands
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "TD" -> CountryDemographics(// Chad
      languages = Seq("Francophone","Arab"),
      cultures = Seq("Sunni"),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "TF" -> CountryDemographics(// French Southern Territories
      languages = Seq("Francophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "TG" -> CountryDemographics(// Togo
      languages = Seq("Francophone"),
      cultures = Seq("Sunni"),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "TH" -> CountryDemographics(// Thailand
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq("ASEAN","Belt & Road"),
      preferredVacationDestinations = Seq()
    ),
    "TJ" -> CountryDemographics(// Tajikistan
      languages = Seq(),
      cultures = Seq("Sunni"),
      geopoliticalBlocs = Seq("CIS"),
      preferredVacationDestinations = Seq()
    ),
    "TK" -> CountryDemographics(// Tokelau
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "TL" -> CountryDemographics(// Timor-Leste
      languages = Seq("Lusophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "TM" -> CountryDemographics(// Turkmenistan
      languages = Seq("Turkic"),
      cultures = Seq("Sunni"),
      geopoliticalBlocs = Seq("CIS"),
      preferredVacationDestinations = Seq()
    ),
    "TN" -> CountryDemographics(// Tunisia
      languages = Seq("Francophone","Arab"),
      cultures = Seq("Sunni"),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "TO" -> CountryDemographics(// Tonga
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "TR" -> CountryDemographics(// Turkey
      languages = Seq("Turkic"),
      cultures = Seq("Sunni"),
      geopoliticalBlocs = Seq("Belt & Road","BRICS","OECD"),
      preferredVacationDestinations = Seq()
    ),
    "TT" -> CountryDemographics(// Trinidad and Tobago
      languages = Seq("Anglophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "TV" -> CountryDemographics(// Tuvalu
      languages = Seq("Anglophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "TW" -> CountryDemographics(// Taiwan
      languages = Seq("Anglophone","Sinophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq("OECD"),
      preferredVacationDestinations = Seq("JP","KR","TH","US","GU")
    ),
    "TZ" -> CountryDemographics(// Tanzania
      languages = Seq("Anglophone","Swahili"),
      cultures = Seq("Sunni"),
      geopoliticalBlocs = Seq("COMESA"),
      preferredVacationDestinations = Seq()
    ),
    "UA" -> CountryDemographics(// Ukraine
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "UG" -> CountryDemographics(// Uganda
      languages = Seq("Anglophone","Swahili"),
      cultures = Seq(),
      geopoliticalBlocs = Seq("COMESA"),
      preferredVacationDestinations = Seq()
    ),
    "UM" -> CountryDemographics(// United States Minor Outlying Islands
      languages = Seq("Anglophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "US" -> CountryDemographics(// United States
      languages = Seq("Anglophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq("NAFTA","APEC","OECD"),
      preferredVacationDestinations = Seq("MX","DO","JM","CR","BS")
    ),
    "UY" -> CountryDemographics(// Uruguay
      languages = Seq("Hispanic"),
      cultures = Seq(),
      geopoliticalBlocs = Seq("MERCOSUR"),
      preferredVacationDestinations = Seq()
    ),
    "UZ" -> CountryDemographics(// Uzbekistan
      languages = Seq("Turkic","Russian"),
      cultures = Seq("Sunni"),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "VC" -> CountryDemographics(// St. Vincent and the Grenadines
      languages = Seq("Anglophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "VE" -> CountryDemographics(// Venezuela, RB
      languages = Seq("Hispanic"),
      cultures = Seq(),
      geopoliticalBlocs = Seq("OPEC"),
      preferredVacationDestinations = Seq()
    ),
    "VG" -> CountryDemographics(// British Virgin Islands
      languages = Seq("Anglophone"),
      cultures = Seq("Commonwealth"),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "VI" -> CountryDemographics(// Virgin Islands (U.S.)
      languages = Seq("Anglophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq("NAFTA"),
      preferredVacationDestinations = Seq()
    ),
    "VN" -> CountryDemographics(// Viet Nam
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq("ASEAN"),
      preferredVacationDestinations = Seq("TH","CN","ID")
    ),
    "VU" -> CountryDemographics(// Vanuatu
      languages = Seq("Anglophone","Francophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "WF" -> CountryDemographics(// Wallis and Futuna
      languages = Seq("Francophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "WS" -> CountryDemographics(// Samoa
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "XK" -> CountryDemographics(// Kosovo
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "YE" -> CountryDemographics(// Yemen, Rep.
      languages = Seq("Arab"),
      cultures = Seq("Sunni","Shia"),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "YT" -> CountryDemographics(// Mayotte
      languages = Seq(),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "ZA" -> CountryDemographics(// South Africa
      languages = Seq("Anglophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq("BRICS"),
      preferredVacationDestinations = Seq()
    ),
    "ZM" -> CountryDemographics(// Zambia
      languages = Seq("Anglophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
    "ZW" -> CountryDemographics(// Zimbabwe
      languages = Seq("Anglophone"),
      cultures = Seq(),
      geopoliticalBlocs = Seq(),
      preferredVacationDestinations = Seq()
    ),
  )

  
  // Utility method for data retrieval
  def getCountryDemographics(countryCode: String): Option[CountryDemographics] = {
    countryDemographics.get(countryCode.toUpperCase)
  }
}