package com.patson.data.airplane

import scala.collection.mutable.ListBuffer
import com.patson.data.Constants._
import com.patson.model.airplane._
import com.patson.data.Meta
import com.patson.util.AirplaneModelDiscountCache

import java.sql.{ResultSet, Types}
import scala.collection.mutable

object ModelSource {
  private[this] val BASE_QUERY = "SELECT * FROM " + AIRPLANE_MODEL_TABLE

  def loadAllModels(): List[Model] = loadModelsByCriteria(List.empty)

  def loadModelsByCriteria(criteria: List[(String, Any)]): List[Model] = {
    val queryString = new StringBuilder(BASE_QUERY)
    if (criteria.nonEmpty) {
      queryString.append(" WHERE ")
      criteria.dropRight(1).foreach(c => queryString.append(c._1 + " = ? AND "))
      queryString.append(criteria.last._1 + " = ?")
    }
    loadModelsByQuery(queryString.toString, criteria.map(_._2))
  }

  def loadModelsByQuery(queryString: String, parameters: Seq[Any] = Seq.empty): List[Model] = {
    val connection = Meta.getConnection()
    val preparedStatement = connection.prepareStatement(queryString)
    parameters.zipWithIndex.foreach { case (p, i) => preparedStatement.setObject(i + 1, p) }

    val resultSet = preparedStatement.executeQuery()
    val models = new ListBuffer[Model]()
    while (resultSet.next()) {
      models += getModelFromRow(resultSet)
    }
    resultSet.close()
    preparedStatement.close()
    connection.close()
    models.toList
  }

  def getModelFromRow(resultSet: ResultSet): Model = {
    val model = Model(
      name             = resultSet.getString("name"),
      family           = resultSet.getString("family"),
      capacity         = resultSet.getInt("capacity"),
      fuelBurn         = resultSet.getInt("fuel_burn"),
      fuelBurnClimb    = resultSet.getInt("fuel_burn_climb"),
      speed            = resultSet.getInt("speed"),
      range            = resultSet.getInt("fly_range"),
      price            = resultSet.getInt("price"),
      lifespan         = resultSet.getInt("lifespan"),
      constructionTime = resultSet.getInt("construction_time"),
      manufacturer     = Manufacturer(resultSet.getString("manufacturer"), resultSet.getString("country_code")),
      runwayRequirement = resultSet.getInt("runway_requirement"),
      airplaneType     = Model.Type.withName(resultSet.getString("airplane_type")),
      introYear        = resultSet.getInt("intro_year"),
      introWeek        = resultSet.getInt("intro_week"),
      climbRateMMin    = resultSet.getInt("climb_rate_mmin"),
      cruiseAltitudeM  = resultSet.getInt("cruise_altitude_m"),
      imageUrl         = resultSet.getString("image_url")
    )
    model.id = resultSet.getInt("id")
    model
  }

  def loadModelById(id: Int): Option[Model] = {
    val result = loadModelsByCriteria(List(("id", id)))
    if (result.isEmpty) None else Some(result.head)
  }

  def loadModelsWithinRange(range: Int): List[Model] = {
    loadModelsByQuery(BASE_QUERY + " WHERE fly_range >= ?", Seq(range))
  }

  // ── Write operations ──────────────────────────────────────────────────────

  private val INSERT_SQL =
    s"""INSERT INTO $AIRPLANE_MODEL_TABLE
       |(name, family, capacity, fuel_burn, fuel_burn_climb, speed, fly_range, price,
       | lifespan, construction_time, country_code, manufacturer, image_url,
       | runway_requirement, intro_year, intro_week, airplane_type,
       | climb_rate_mmin, cruise_altitude_m)
       |VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""".stripMargin

  private val UPDATE_SQL =
    s"""UPDATE $AIRPLANE_MODEL_TABLE SET
       | family = ?, capacity = ?, fuel_burn = ?, fuel_burn_climb = ?, speed = ?,
       | fly_range = ?, price = ?, lifespan = ?, construction_time = ?,
       | country_code = ?, manufacturer = ?, image_url = ?, runway_requirement = ?,
       | intro_year = ?, intro_week = ?, airplane_type = ?,
       | climb_rate_mmin = ?, cruise_altitude_m = ?
       |WHERE name = ?""".stripMargin

  def saveModels(models: List[Model]): Unit = {
    val connection = Meta.getConnection()
    val ps = connection.prepareStatement(INSERT_SQL)
    connection.setAutoCommit(false)
    models.foreach { m =>
      ps.setString(1,  m.name)
      ps.setString(2,  m.family)
      ps.setInt(3,     m.capacity)
      ps.setInt(4,     m.fuelBurn)
      ps.setInt(5,     m.fuelBurnClimb)
      ps.setInt(6,     m.speed)
      ps.setInt(7,     m.range)
      ps.setInt(8,     m.price)
      ps.setInt(9,     m.lifespan)
      ps.setInt(10,    m.constructionTime)
      ps.setString(11, m.manufacturer.countryCode)
      ps.setString(12, m.manufacturer.name)
      ps.setString(13, m.imageUrl)
      ps.setInt(14,    m.runwayRequirement)
      ps.setInt(15,    m.introYear)
      ps.setInt(16,    m.introWeek)
      ps.setString(17, m.airplaneType.toString)
      ps.setInt(18,    m.climbRateMMin)
      ps.setInt(19,    m.cruiseAltitudeM)
      ps.executeUpdate()
    }
    ps.close()
    connection.commit()
    connection.close()
  }

  /**
   * UPDATE-only; never deletes rows. Safe to run against a live DB with active
   * airplane instances. Models not present in the supplied list are untouched.
   * Models in the list whose name does not yet exist are inserted.
   */
  def upsertModels(models: List[Model]): Unit = {
    val connection = Meta.getConnection()
    val updatePs = connection.prepareStatement(UPDATE_SQL)
    val insertPs = connection.prepareStatement(INSERT_SQL)

    // Fetch existing names in one query to decide insert vs update.
    val existingNames: Set[String] = {
      val st = connection.prepareStatement(s"SELECT name FROM $AIRPLANE_MODEL_TABLE")
      val rs = st.executeQuery()
      val buf = scala.collection.mutable.HashSet[String]()
      while (rs.next()) buf += rs.getString("name")
      rs.close(); st.close()
      buf.toSet
    }

    connection.setAutoCommit(false)
    models.foreach { m =>
      if (existingNames.contains(m.name)) {
        // UPDATE – positional params match UPDATE_SQL order
        updatePs.setString(1,  m.family)
        updatePs.setInt(2,     m.capacity)
        updatePs.setInt(3,     m.fuelBurn)
        updatePs.setInt(4,     m.fuelBurnClimb)
        updatePs.setInt(5,     m.speed)
        updatePs.setInt(6,     m.range)
        updatePs.setInt(7,     m.price)
        updatePs.setInt(8,     m.lifespan)
        updatePs.setInt(9,     m.constructionTime)
        updatePs.setString(10, m.manufacturer.countryCode)
        updatePs.setString(11, m.manufacturer.name)
        updatePs.setString(12, m.imageUrl)
        updatePs.setInt(13,    m.runwayRequirement)
        updatePs.setInt(14,    m.introYear)
        updatePs.setInt(15,    m.introWeek)
        updatePs.setString(16, m.airplaneType.toString)
        updatePs.setInt(17,    m.climbRateMMin)
        updatePs.setInt(18,    m.cruiseAltitudeM)
        updatePs.setString(19, m.name)  // WHERE clause
        updatePs.executeUpdate()
      } else {
        // INSERT – same positional params as saveModels
        insertPs.setString(1,  m.name)
        insertPs.setString(2,  m.family)
        insertPs.setInt(3,     m.capacity)
        insertPs.setInt(4,     m.fuelBurn)
        insertPs.setInt(5,     m.fuelBurnClimb)
        insertPs.setInt(6,     m.speed)
        insertPs.setInt(7,     m.range)
        insertPs.setInt(8,     m.price)
        insertPs.setInt(9,     m.lifespan)
        insertPs.setInt(10,    m.constructionTime)
        insertPs.setString(11, m.manufacturer.countryCode)
        insertPs.setString(12, m.manufacturer.name)
        insertPs.setString(13, m.imageUrl)
        insertPs.setInt(14,    m.runwayRequirement)
        insertPs.setInt(15,    m.introYear)
        insertPs.setInt(16,    m.introWeek)
        insertPs.setString(17, m.airplaneType.toString)
        insertPs.setInt(18,    m.climbRateMMin)
        insertPs.setInt(19,    m.cruiseAltitudeM)
        insertPs.executeUpdate()
      }
    }
    updatePs.close()
    insertPs.close()
    connection.commit()
    connection.close()
  }

  // Kept for backward compatibility; delegates to upsertModels.
  def updateModels(models: List[Model]): Unit = upsertModels(models)

  def deleteAllModels(): Int = {
    val connection = Meta.getConnection()
    val ps = connection.prepareStatement("DELETE FROM " + AIRPLANE_MODEL_TABLE)
    val deletedCount = ps.executeUpdate()
    ps.close()
    connection.close()
    println("Deleted " + deletedCount + " model records")
    deletedCount
  }

  // ── Favorite / Discount methods unchanged below ───────────────────────────
  // (all the saveFavoriteModelId, loadFavoriteModelId, saveAirlineDiscount,
  //  deleteAirlineDiscount, loadAll*Discounts, etc. remain as-is)

  def saveFavoriteModelId(airlineId: Int, modelId: Int, startCycle: Int): Unit = {
    val connection = Meta.getConnection()
    val ps = connection.prepareStatement(
      "REPLACE INTO " + AIRPLANE_MODEL_FAVORITE_TABLE + "(airline, model, start_cycle) VALUES(?,?,?)")
    connection.setAutoCommit(false)
    ps.setInt(1, airlineId); ps.setInt(2, modelId); ps.setInt(3, startCycle)
    ps.executeUpdate(); ps.close(); connection.commit(); connection.close()
  }

  def loadFavoriteModelId(airlineId: Int): Option[(Int, Int)] = {
    val connection = Meta.getConnection()
    val ps = connection.prepareStatement(
      "SELECT * FROM " + AIRPLANE_MODEL_FAVORITE_TABLE + " WHERE airline = ?")
    try {
      ps.setInt(1, airlineId)
      val rs = ps.executeQuery()
      val result = if (rs.next()) Some((rs.getInt("model"), rs.getInt("start_cycle"))) else None
      rs.close(); result
    } finally { ps.close(); connection.close() }
  }

  def deleteAllFavoriteModelIds(): Unit = {
    val connection = Meta.getConnection()
    val ps = connection.prepareStatement("DELETE FROM " + AIRPLANE_MODEL_FAVORITE_TABLE)
    try { ps.executeUpdate() } finally { ps.close(); connection.close() }
  }

  def saveAirlineDiscount(airlineId: Int, discount: ModelDiscount): Unit = {
    val connection = Meta.getConnection()
    val ps = connection.prepareStatement(
      "REPLACE INTO " + AIRPLANE_MODEL_AIRLINE_DISCOUNT_TABLE +
        "(airline, model, discount, discount_type, discount_reason, expiration_cycle) VALUES(?,?,?,?,?,?)")
    connection.setAutoCommit(false)
    ps.setInt(1, airlineId); ps.setInt(2, discount.modelId)
    ps.setDouble(3, discount.discount); ps.setInt(4, discount.discountType.id)
    ps.setInt(5, discount.discountReason.id)
    discount.expirationCycle match {
      case Some(c) => ps.setInt(6, c)
      case None    => ps.setNull(6, Types.INTEGER)
    }
    ps.executeUpdate(); ps.close(); connection.commit(); connection.close()
  }

  def deleteAirlineDiscount(airlineId: Int, modelId: Int, discountReason: DiscountReason.Value): Unit = {
    val connection = Meta.getConnection()
    val ps = connection.prepareStatement(
      "DELETE FROM " + AIRPLANE_MODEL_AIRLINE_DISCOUNT_TABLE +
        " WHERE airline = ? AND model = ? AND discount_reason = ?")
    connection.setAutoCommit(false)
    ps.setInt(1, airlineId); ps.setInt(2, modelId); ps.setInt(3, discountReason.id)
    ps.executeUpdate(); ps.close(); connection.commit(); connection.close()
  }

  def deleteAllAirlineDiscounts(): Unit = {
    val connection = Meta.getConnection()
    val ps = connection.prepareStatement("DELETE FROM " + AIRPLANE_MODEL_AIRLINE_DISCOUNT_TABLE)
    ps.executeUpdate(); connection.close()
  }

  def loadAllAirlineDiscounts(): Map[Int, List[ModelDiscount]] =
    loadAirlineDiscountsByCriteria(List.empty)

  def loadAirlineDiscountsByAirlineId(airlineId: Int): List[ModelDiscount] =
    loadAirlineDiscountsByCriteria(List(("airline", airlineId))).getOrElse(airlineId, List.empty)

  def loadAirlineDiscountsByAirlineIdAndModelId(airlineId: Int, modelId: Int): List[ModelDiscount] =
    loadAirlineDiscountsByCriteria(List(("airline", airlineId), ("model", modelId))).getOrElse(airlineId, List.empty)

  def loadAirlineDiscountsByCriteria(criteria: List[(String, Any)]): Map[Int, List[ModelDiscount]] = {
    val qs = new StringBuilder("SELECT * FROM " + AIRPLANE_MODEL_AIRLINE_DISCOUNT_TABLE)
    if (criteria.nonEmpty) {
      qs.append(" WHERE ")
      criteria.dropRight(1).foreach(c => qs.append(c._1 + " = ? AND "))
      qs.append(criteria.last._1 + " = ?")
    }
    loadAirlineDiscountsByQuery(qs.toString, criteria.map(_._2))
  }

  def loadAirlineDiscountsByQuery(queryString: String, parameters: Seq[Any] = Seq.empty): Map[Int, List[ModelDiscount]] = {
    val connection = Meta.getConnection()
    val ps = connection.prepareStatement(queryString)
    parameters.zipWithIndex.foreach { case (p, i) => ps.setObject(i + 1, p) }
    val rs = ps.executeQuery()
    val result = new mutable.HashMap[Int, ListBuffer[ModelDiscount]]()
    while (rs.next()) {
      val airlineId = rs.getInt("airline")
      val buf = result.getOrElseUpdate(airlineId, ListBuffer())
      val expObj = rs.getObject("expiration_cycle")
      buf.append(ModelDiscount(
        rs.getInt("model"),
        rs.getDouble("discount"),
        DiscountType(rs.getInt("discount_type")),
        DiscountReason(rs.getInt("discount_reason")),
        if (expObj == null) None else Some(expObj.asInstanceOf[Int])
      ))
    }
    rs.close(); ps.close(); connection.close()
    result.view.mapValues(_.toList).toMap
  }

  def updateModelDiscounts(discounts: List[ModelDiscount]): Unit = {
    val connection = Meta.getConnection()
    val purge = connection.prepareStatement("DELETE FROM " + AIRPLANE_MODEL_DISCOUNT_TABLE)
    purge.executeUpdate(); purge.close()
    val ps = connection.prepareStatement(
      "REPLACE INTO " + AIRPLANE_MODEL_DISCOUNT_TABLE +
        "(model, discount, discount_type, discount_reason, expiration_cycle) VALUES(?,?,?,?,?)")
    connection.setAutoCommit(false)
    discounts.foreach { d =>
      ps.setInt(1, d.modelId); ps.setDouble(2, d.discount)
      ps.setInt(3, d.discountType.id); ps.setInt(4, d.discountReason.id)
      d.expirationCycle match {
        case Some(c) => ps.setInt(5, c)
        case None    => ps.setNull(5, Types.INTEGER)
      }
      ps.executeUpdate()
    }
    AirplaneModelDiscountCache.updateModelDiscounts(discounts)
    ps.close(); connection.commit(); connection.close()
  }

  def loadAllModelDiscounts(): List[ModelDiscount] = loadModelDiscountsByCriteria(List.empty)

  def loadModelDiscountsByModelId(modelId: Int): List[ModelDiscount] =
    loadModelDiscountsByCriteria(List(("model", modelId)))

  def loadModelDiscountsByCriteria(criteria: List[(String, Any)]): List[ModelDiscount] = {
    val qs = new StringBuilder("SELECT * FROM " + AIRPLANE_MODEL_DISCOUNT_TABLE)
    if (criteria.nonEmpty) {
      qs.append(" WHERE ")
      criteria.dropRight(1).foreach(c => qs.append(c._1 + " = ? AND "))
      qs.append(criteria.last._1 + " = ?")
    }
    loadModelDiscountsByQuery(qs.toString, criteria.map(_._2))
  }

  def loadModelDiscountsByQuery(queryString: String, parameters: Seq[Any] = Seq.empty): List[ModelDiscount] = {
    val connection = Meta.getConnection()
    val ps = connection.prepareStatement(queryString)
    parameters.zipWithIndex.foreach { case (p, i) => ps.setObject(i + 1, p) }
    val rs = ps.executeQuery()
    val buf = ListBuffer[ModelDiscount]()
    while (rs.next()) {
      val expObj = rs.getObject("expiration_cycle")
      buf.append(ModelDiscount(
        rs.getInt("model"),
        rs.getDouble("discount"),
        DiscountType(rs.getInt("discount_type")),
        DiscountReason(rs.getInt("discount_reason")),
        if (expObj == null) None else Some(expObj.asInstanceOf[Int])
      ))
    }
    rs.close(); ps.close(); connection.close()
    buf.toList
  }
}