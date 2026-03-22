package com.patson.model.airplane

import com.patson.model.IdObject

/**
 * Represents a single airframe on order in the production queue.
 * One row per airframe — quantity N order inserts N rows.
 *
 * @param modelId       the ordered model
 * @param airlineId     the purchasing airline
 * @param orderCycle    game cycle on which the order was placed
 * @param shuffleIndex  random ordering within the same (modelId, orderCycle) batch;
 *                      re-randomised each time a new order lands on the same cycle
 * @param homeAirportId the airport this airframe will be assigned to on delivery
 * @param id            DB primary key
 */
case class ModelOrderQueue(
  modelId:       Int,
  airlineId:     Int,
  orderCycle:    Int,
  shuffleIndex:  Int,
  homeAirportId: Int,
  id:            Int = 0
) extends IdObject