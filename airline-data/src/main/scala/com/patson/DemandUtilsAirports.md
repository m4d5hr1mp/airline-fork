package com.patson

import com.patson.DemandUtilsDemographics.CountryDemographics
import scala.collection.immutable.ListMap

object DemandUtilsAirports {

  case class AirportData(
    diasporas: Seq[String],                  // ISO2 country codes for diasporas at this airport
    economicAffinities: Seq[String],         // Economic sectors influencing traffic (from referenceAffinities)
    additionalLanguages: Option[Seq[String]] = None,  // Airport-specific languages not national
    additionalCultures: Option[Seq[String]] = None,   // Airport-specific cultures not national
    additionalGeopoliticalBlocs: Option[Seq[String]] = None,  // Airport-specific geopolitical ties not national
    gdpMultiplier: Option[Double] = None     // Local GDP-capita multiplier relative to national average
  )

  // Reference for economic affinities (unchanged)
  val referenceAffinities: Set[String] = Set(
    // ===== FINANCE SECTOR =====
    "Global Finance", "NA Finance", "EU Finance", "Asia Finance", "LATAM Finance", "MENA Finance", "CIS Finance",
    "Banking", "Venture Capital", "Insurance",
    // ===== PROFESSIONAL SERVICES =====
    "Consulting", "Professional Services",
    // ===== TECHNOLOGY =====
    "Software & IT", "Microelectronics", "Telco",
    // ===== MANUFACTURING & INDUSTRIAL =====
    "Automotive", "Industrial Automation", "Heavy Industry", "Specialty Manufacturing", "Consumer Goods", "Aerospace",
    "Oil & Gas", "Petrochemicals", "Mining",
    // ===== ENERGY =====
    "Renewables",
    // ===== DEFENSE =====
    "Defense NATO", "Defense RU", "Defense CN",
    // ===== LIFE SCIENCES =====
    "Biotech & Pharma",
    // ===== MEDIA =====
    "Media & Entertainment",
    // ===== GOVERNMENT =====
    "Government",
    // ===== LOGISTICS & TRANSPORTATION =====
    "Shipbuilding", "Maritime Shipping", "Rail Freight",
    "NA Logistics", "EU Logistics", "MENA Logistics", "CIS Logistics", "Belt & Road",
    // ===== RESEARCH & EDUCATION =====
    "Academia", "R&D",
    // ===== SPECIAL CATEGORIES =====
    "Remote Workers", "Retirement Community"
  )

  // Airport data map (corrected example; expand with more entries as needed)
  val airportData: Map[String, AirportData] = ListMap(
    "YYZ" -> AirportData(
      diasporas = Seq("IN", "CN", "PH"),
      economicAffinities = Seq(),  // Corrected from 'affinities'
      additionalLanguages = Some(Seq("Hindi", "Sinophone")),  // Example addition for languages
      additionalCultures = Some(Seq("Hindu", "Sinophone")),  // Existing, but ensured as cultures
      additionalGeopoliticalBlocs = None,
      gdpMultiplier = Some(1.30)
    ),
    // Add more airports here, e.g.:
    "JFK" -> AirportData(
      diasporas = Seq("IT", "IE", "PR"),
      economicAffinities = Seq("Global Finance", "Media & Entertainment"),
      additionalLanguages = Some(Seq("Hispanic")),
      additionalCultures = Some(Seq("Jewish")),
      additionalGeopoliticalBlocs = Some(Seq("OECD")),
      gdpMultiplier = Some(1.50)
    )
    // ... additional entries ...
  )

  // Utility method for data retrieval (unchanged)
  def getAirportData(iata: String): Option[AirportData] =
    airportData.get(iata.toUpperCase)

  // Updated: Return Set[String] for effective cultures, combining country and airport levels
  def getEffectiveCulturalTags(iata: String, countryCode: String): Set[String] = {
    val countryCultures = DemandUtilsDemographics.getCountryDemographics(countryCode)
      .map(_.cultures.toSet)
      .getOrElse(Set.empty)
    
    val airportAdditional = getAirportData(iata)
      .flatMap(_.additionalCultures)
      .map(_.toSet)
      .getOrElse(Set.empty)
    
    countryCultures ++ airportAdditional
  }

  // New: Parallel method for languages
  def getEffectiveLanguageTags(iata: String, countryCode: String): Set[String] = {
    val countryLanguages = DemandUtilsDemographics.getCountryDemographics(countryCode)
      .map(_.languages.toSet)
      .getOrElse(Set.empty)
    
    val airportAdditional = getAirportData(iata)
      .flatMap(_.additionalLanguages)
      .map(_.toSet)
      .getOrElse(Set.empty)
    
    countryLanguages ++ airportAdditional
  }

  // New: Parallel method for geopolitical blocs
  def getEffectiveGeopoliticalBlocs(iata: String, countryCode: String): Set[String] = {
    val countryBlocs = DemandUtilsDemographics.getCountryDemographics(countryCode)
      .map(_.geopoliticalBlocs.toSet)
      .getOrElse(Set.empty)
    
    val airportAdditional = getAirportData(iata)
      .flatMap(_.additionalGeopoliticalBlocs)
      .map(_.toSet)
      .getOrElse(Set.empty)
    
    countryBlocs ++ airportAdditional
  }

  // Combined profile method (unchanged, but now uses updated Demographics)
  def getCountryProfile(countryCode: String): Option[(DemandUtilsEconomics.CountryEconomicIndicators, CountryDemographics)] = {
    for {
      econ <- DemandUtilsEconomics.getCountryEconomicIndicators(countryCode)
      demo <- DemandUtilsDemographics.getCountryDemographics(countryCode)
    } yield (econ, demo)
  }
}