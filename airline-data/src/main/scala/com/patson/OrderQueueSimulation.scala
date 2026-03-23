package com.patson

import com.patson.data.airplane.{ModelSource, OrderQueueSource}
import com.patson.data.{AirlineSource, AirplaneSource}
import com.patson.model.{AirlineCashFlowItem, AirlineTransaction, Airport, CashFlowType, TransactionType}
import com.patson.model.airplane.{Airplane, AirplaneConfiguration, Model}
import com.patson.model.airplane.Model.Type
import com.patson.util.AirlineCache

import scala.collection.mutable.ListBuffer

object OrderQueueSimulation {

  val PRODUCTION_CAPACITY: Map[Type.Value, Int] = Map(
    Type.SHORT_RANGE_PROP -> 25,
    Type.LONG_RANGE_PROP  -> 10,
    Type.SMALL_PROP       -> 10,
    Type.REGIONAL_PROP    -> 10,
    Type.LIGHT            ->  5,
    Type.SMALL            -> 10,
    Type.REGIONAL         -> 15,
    Type.EARLY_JET        -> 15,
    Type.MEDIUM           -> 15,
    Type.LARGE            ->  8,
    Type.X_LARGE          ->  4,
    Type.JUMBO            ->  2,
    Type.SUPERSONIC       ->  2
  )

  def processOrderQueue(currentCycle: Int): Unit = {
    println(s"[OrderQueue] Processing deliveries for cycle $currentCycle")

    val pendingByModel: Map[Int, List[com.patson.model.airplane.ModelOrderQueue]] =
      OrderQueueSource.loadAllPendingRows()
        .groupBy(_.modelId)

    if (pendingByModel.isEmpty) {
      println("[OrderQueue] No deliveries due this cycle")
      return
    }

    val toDeliver    = ListBuffer[com.patson.model.airplane.ModelOrderQueue]()
    val newAirplanes = ListBuffer[Airplane]()

    pendingByModel.foreach { case (modelId, rows) =>
      ModelSource.loadModelById(modelId) match {
        case None =>
          println(s"[OrderQueue] WARNING: model $modelId not found, dropping ${rows.size} orphan rows")
          toDeliver.appendAll(rows)

        case Some(model) =>
          val cap = PRODUCTION_CAPACITY.getOrElse(model.airplaneType, 5)

          val due = rows
            .filter(r => r.orderCycle + model.constructionTime <= currentCycle)
            .sortBy(r => (r.orderCycle, r.shuffleIndex))
            .take(cap)

          due.foreach { row =>
            AirlineCache.getAirline(row.airlineId, fullLoad = true) match {
              case None =>
                println(s"[OrderQueue] WARNING: airline ${row.airlineId} not found, dropping row ${row.id}")
                toDeliver.append(row)

              case Some(airline) =>
                val homeAirport: Airport =
                  if (row.homeAirportId > 0)
                    Airport.fromId(row.homeAirportId)
                  else
                    airline.getHeadQuarter().map(_.airport).getOrElse(Airport.fromId(0))

                val airplane = Airplane(
                  model            = model,
                  owner            = airline,
                  constructedCycle = currentCycle,
                  purchasedCycle   = row.orderCycle,
                  condition        = Airplane.MAX_CONDITION,
                  depreciationRate = 0,
                  value            = model.price,
                  home             = homeAirport,
                  isReady          = true,
                  purchaseRate     = 1.0
                )
                newAirplanes.append(airplane)
                toDeliver.append(row)
            }
          }
      }
    }

    if (newAirplanes.nonEmpty) {
      // saveAirplanes assigns generated DB ids back to each airplane object in-memory
      AirplaneSource.saveAirplanes(newAirplanes.toList)
      println(s"[OrderQueue] Delivered ${newAirplanes.size} airframes")

      // Now that each airplane has an id, assign default seat configuration.
      // assignDefaultConfiguration() saves the config template if needed and
      // sets airplane.configuration in-memory, but does not write the
      // airplane->configuration link. We write those links in one batch after.
      newAirplanes.foreach { airplane =>
        airplane.assignDefaultConfiguration()
      }
      AirplaneSource.saveAirplaneConfigurationLinks(newAirplanes.toList)
      println(s"[OrderQueue] Assigned configurations to ${newAirplanes.size} airframes")
    }

    if (toDeliver.nonEmpty) {
      OrderQueueSource.deleteRows(toDeliver.map(_.id).toList)
      println(s"[OrderQueue] Cleared ${toDeliver.size} queue rows")
    }
  }

  def placeOrder(modelId: Int, airlineId: Int, quantity: Int, orderCycle: Int, homeAirportId: Int): Unit = {
    OrderQueueSource.insertAndReshuffle(modelId, airlineId, quantity, orderCycle, homeAirportId)
  }
}