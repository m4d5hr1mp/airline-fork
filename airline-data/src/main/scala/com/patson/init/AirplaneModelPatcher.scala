package com.patson.init

import com.patson.data.airplane.ModelSource
import com.patson.init.AirplaneModelCsvLoader

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
 * Non-destructive patcher: reads airplane_models.csv and UPSERTs every row.
 *
 * Behaviour contract:
 *   - Models whose name already exists in DB → UPDATE all columns.
 *   - Models whose name does not exist      → INSERT (new models added to game).
 *   - Models in DB not present in CSV       → LEFT ALONE (no deletes).
 *   - Active airplane instances             → NEVER touched; model FK is stable.
 *
 * Safe to run on a live production database at any time.
 *
 * Usage:
 *   sbt "runMain com.patson.init.AirplaneModelPatcher"
 *   or pass -Dairplane.models.csv=/path/to/file to override CSV location.
 */
object AirplaneModelPatcher extends App {
  mainFlow()

  def mainFlow(): Unit = {
    patch()
    Await.result(actorSystem.terminate(), Duration.Inf)
  }

  def patch(): Unit = {
    val csvPath = System.getProperty("airplane.models.csv", AirplaneModelCsvLoader.DEFAULT_CSV_PATH)
    println(s"[AirplaneModelPatcher] Loading models from: $csvPath")

    AirplaneModelCsvLoader.loadFromFile(csvPath) match {
      case Left(errors) =>
        errors.foreach(e => println(s"  CSV ERROR line ${e.line}: ${e.message}"))
        throw new RuntimeException(
          s"[AirplaneModelPatcher] Aborting: ${errors.size} CSV parse error(s). No changes made.")

      case Right(models) =>
        println(s"[AirplaneModelPatcher] Parsed ${models.size} models; applying non-destructive upsert")
        ModelSource.upsertModels(models)
        println(s"[AirplaneModelPatcher] Done — existing airplanes and DB rows not in CSV are untouched")
    }
  }
}