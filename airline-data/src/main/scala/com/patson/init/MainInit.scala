package com.patson.init

import com.patson.data.Meta

/**
 * The main flow to initialize everything.
 *
 * Relation generation is handled by WorldStateGenerator.runInit() which reads
 * events from the geopolitical_event table (seeded by GeopoliticalEventLoader).
 * CountryMutualRelationshipGenerator is retired — see .md file for reference.
 */
object MainInit extends App {
  Meta.createSchema()
  GeoDataGenerator.mainFlow()
  AirplaneModelInitializer.mainFlow()
  GenericTransitGenerator.generateGenericTransit()
  GeopoliticalEventLoader.loadFromCsv("init_events.csv")
  WorldStateGenerator.runInit()
}