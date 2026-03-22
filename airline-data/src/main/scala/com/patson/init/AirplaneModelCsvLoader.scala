package com.patson.init

import com.patson.model.airplane.{Manufacturer, Model}

import java.io.{BufferedReader, FileReader}
import scala.util.{Try, Using}

/**
 * Parses the canonical airplane_models.csv into Model instances.
 *
 * CSV column order (header row required, order is fixed):
 *
 *   manufacturer, country_code, family, name, airplane_type,
 *   intro_year, intro_week, price, capacity, range,
 *   fuel_burn, fuel_burn_climb, speed, runway_requirement,
 *   climb_rate, cruise_altitude, lifespan, construction_time, image_url
 *
 * Rules:
 *   - First row is a header and is skipped.
 *   - Fields may be quoted with double-quotes (RFC 4180).
 *   - Blank lines and lines starting with '#' are ignored.
 *   - airplane_type must exactly match one of Model.Type's Value names
 *     (e.g. MEDIUM, EARLY_JET, LONG_RANGE_PROP, …).
 *   - lifespan is stored in the CSV as years; converted to weeks on load.
 *   - fuel_burn and fuel_burn_climb are already in fuel-units/min (integer).
 */
object AirplaneModelCsvLoader {

  val DEFAULT_CSV_PATH = "data/airplane_models.csv"

  case class ParseError(line: Int, message: String)

  /**
   * Load models from a CSV file.
   * @return Right(models) on success, Left(errors) if any row fails to parse.
   *         Partial results are NOT returned on error; the caller should abort.
   */
  def loadFromFile(path: String = DEFAULT_CSV_PATH): Either[List[ParseError], List[Model]] = {
    val errors  = scala.collection.mutable.ListBuffer[ParseError]()
    val models  = scala.collection.mutable.ListBuffer[Model]()
    var lineNum = 0

    Using(new BufferedReader(new FileReader(path))) { reader =>
      var line: String = reader.readLine()
      lineNum += 1 // header

      line = reader.readLine()
      lineNum += 1

      while (line != null) {
        val trimmed = line.trim
        if (trimmed.nonEmpty && !trimmed.startsWith("#")) {
          parseRow(trimmed, lineNum) match {
            case Right(model) => models += model
            case Left(err)    => errors += err
          }
        }
        line = reader.readLine()
        lineNum += 1
      }
    }.recover { case e => errors += ParseError(0, s"File read error: ${e.getMessage}") }

    if (errors.nonEmpty) Left(errors.toList)
    else Right(models.toList)
  }

  // ── Internal ──────────────────────────────────────────────────────────────

  private def parseRow(line: String, lineNum: Int): Either[ParseError, Model] = {
    val fields = splitCsvLine(line)
    if (fields.length < 19)
      return Left(ParseError(lineNum, s"Expected 19 fields, got ${fields.length}: $line"))

    Try {
      val manufacturer     = fields(0)
      val countryCode      = fields(1)
      val family           = fields(2)
      val name             = fields(3)
      val airplaneType     = Model.Type.withName(fields(4).trim.toUpperCase)
      val introYear        = fields(5).toInt
      val introWeek        = fields(6).toInt
      val price            = fields(7).toInt
      val capacity         = fields(8).toInt
      val flyRange         = fields(9).toInt
      val fuelBurn         = (fields(10).toInt / 60.0).toInt
      val fuelBurnClimb    = (fields(11).toInt / 60.0).toInt
      val speed            = fields(12).toInt
      val runwayReq        = fields(13).toInt
      val climbRate        = fields(14).toInt
      val cruiseAltitude   = fields(15).toInt
      val lifespanYears    = fields(16).toInt
      val constructionTime = fields(17).toInt
      val imageUrl         = if (fields.length > 18) fields(18) else ""

      Model(
        name             = name,
        family           = family,
        capacity         = capacity,
        fuelBurn         = fuelBurn,
        fuelBurnClimb    = fuelBurnClimb,
        speed            = speed,
        range            = flyRange,
        price            = price,
        lifespan         = lifespanYears * 52,
        constructionTime = constructionTime,
        manufacturer     = Manufacturer(manufacturer, countryCode),
        runwayRequirement = runwayReq,
        airplaneType     = airplaneType,
        introYear        = introYear,
        introWeek        = introWeek,
        climbRateMMin    = climbRate,
        cruiseAltitudeM  = cruiseAltitude,
        imageUrl         = imageUrl
      )
    }.toEither.left.map(e => ParseError(lineNum, s"Parse error: ${e.getMessage} — $line"))
  }

  /**
   * Minimal RFC 4180 CSV split. Handles quoted fields containing commas.
   * Does not handle escaped double-quotes within quoted fields (\"\" → ").
   * Good enough for our controlled-format data files.
   */
  private def splitCsvLine(line: String): Array[String] = {
    val fields  = scala.collection.mutable.ArrayBuffer[String]()
    val current = new StringBuilder
    var inQuotes = false

    line.foreach {
      case '"'             => inQuotes = !inQuotes
      case ',' if !inQuotes =>
        fields += current.toString().trim
        current.clear()
      case c               => current.append(c)
    }
    fields += current.toString().trim
    fields.toArray
  }
}