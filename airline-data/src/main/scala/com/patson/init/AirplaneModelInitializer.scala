package com.patson.init

import com.patson.data.airplane.ModelSource
import com.patson.init.AirplaneModelCsvLoader

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
 * Full initializer: wipes all model rows and re-inserts from CSV.
 * Only for fresh DB setup or full model resets.
 * NOT safe on a live DB with active airplanes.
 *
 * For hot-patching existing production data use AirplaneModelPatcher instead.
 */
object AirplaneModelInitializer extends App {
  mainFlow()

  def mainFlow(): Unit = {
    populateAirplaneModels()
    Await.result(actorSystem.terminate(), Duration.Inf)
  }

  def populateAirplaneModels(): Unit = {
    val csvPath = System.getProperty("airplane.models.csv", AirplaneModelCsvLoader.DEFAULT_CSV_PATH)
    println(s"[AirplaneModelInitializer] Loading models from: $csvPath")

    AirplaneModelCsvLoader.loadFromFile(csvPath) match {
      case Left(errors) =>
        errors.foreach(e => println(s"  CSV ERROR line ${e.line}: ${e.message}"))
        throw new RuntimeException(
          s"[AirplaneModelInitializer] Aborting: ${errors.size} CSV parse error(s)")

      case Right(models) =>
        println(s"[AirplaneModelInitializer] Parsed ${models.size} models; dropping existing rows and re-inserting")
        ModelSource.deleteAllModels()
        ModelSource.saveModels(models)
        println(s"[AirplaneModelInitializer] Done")
    }
  }
}