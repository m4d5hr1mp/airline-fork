package com.patson.model.airplane

import com.patson.data.airplane.ModelSource
import com.patson.model.airplane.Model.Type
import com.patson.util.{AirplaneModelCache, AirplaneModelDiscountCache}

import scala.collection.mutable.ListBuffer

case class ModelDiscount(modelId: Int, discount: Double, discountType: DiscountType.Value, discountReason: DiscountReason.Value, expirationCycle: Option[Int]) {
  val description: String = discountReason match {
    case DiscountReason.OBSOLESCENCE  => s"${(discount * 100).toInt}% off price — aging model"
    case DiscountReason.LOW_DEMAND    => s"${(discount * 100).toInt}% off ${DiscountType.description(discountType)} due to low demand"
    case DiscountReason.BULK_ORDER    => s"${(discount * 100).toInt}% off price — bulk order"
  }
}

object ModelDiscount {

  // ── Obsolescence: residual price floor by Type ───────────────────────────
  // Types set to 1.0 never decay (price always stays at full).
  val RESIDUAL_FRACTION: Map[Type.Value, Double] = Map(
    Type.SHORT_RANGE_PROP -> 1.0,   // no decay
    Type.LONG_RANGE_PROP  -> 1.0,   // no decay
    Type.SMALL_PROP       -> 0.50,
    Type.REGIONAL_PROP    -> 0.45,
    Type.LIGHT            -> 1.0,   // no decay
    Type.SMALL            -> 0.50,
    Type.REGIONAL         -> 0.60,
    Type.EARLY_JET        -> 0.80,
    Type.MEDIUM           -> 0.60,
    Type.LARGE            -> 0.55,
    Type.X_LARGE          -> 0.50,
    Type.JUMBO            -> 0.65,
    Type.SUPERSONIC       -> 1.0    // no decay
  )

  // ── Bulk order discount brackets by Type ─────────────────────────────────
  // (minQty, minDiscount, maxQty, maxDiscount)
  // None means no bulk discount for this type.
  val BULK_DISCOUNT_BRACKET: Map[Type.Value, Option[(Int, Double, Int, Double)]] = Map(
    Type.SHORT_RANGE_PROP -> Some((10, 0.05, 30, 0.25)),
    Type.LONG_RANGE_PROP  -> Some((10, 0.10, 30, 0.25)),
    Type.SMALL_PROP       -> Some((10, 0.05, 30, 0.25)),
    Type.REGIONAL_PROP    -> Some((10, 0.05, 30, 0.25)),
    Type.LIGHT            -> None,
    Type.SMALL            -> Some((10, 0.05, 30, 0.25)),
    Type.REGIONAL         -> Some((15, 0.05, 50, 0.35)),
    Type.EARLY_JET        -> Some((25, 0.10, 75, 0.35)),
    Type.MEDIUM           -> Some((25, 0.10, 75, 0.35)),
    Type.LARGE            -> Some((25, 0.10, 50, 0.35)),
    Type.X_LARGE          -> Some((25, 0.10, 50, 0.20)),
    Type.JUMBO            -> Some((15, 0.075, 50, 0.225)),
    Type.SUPERSONIC       -> None
  )

  // ── Obsolescence discount (purely computed, no DB) ────────────────────────
  /**
   * Returns a price discount for model age if applicable.
   * Guard: models with availabilityCycle <= 0 (pre-1955 Week 1) are exempt.
   * Decay shape: quadratic (k=2), starts after first 1/4 of lifespan, reaches
   * residualFraction at end of lifespan.
   */
  def computeObsolescenceDiscount(model: Model, currentCycle: Int): Option[ModelDiscount] = {
    val residualFraction = RESIDUAL_FRACTION.getOrElse(model.airplaneType, 1.0)

    // No decay for types with residual = 1.0 or pre-world-start models
    if (residualFraction >= 1.0 || model.availabilityCycle <= 0) return None

    val graceLength = model.lifespan / 4
    val decayLength = model.lifespan - graceLength
    val elapsed     = currentCycle - model.availabilityCycle - graceLength

    if (elapsed <= 0) return None // still in grace period

    val t           = math.min(1.0, elapsed.toDouble / decayLength)
    val multiplier  = 1.0 - (1.0 - residualFraction) * t * t  // quadratic k=2
    val discount    = 1.0 - multiplier

    if (discount <= 0.0) None
    else Some(ModelDiscount(model.id, discount, DiscountType.PRICE, DiscountReason.OBSOLESCENCE, None))
  }

  // ── Bulk order discount (computed at purchase time, not stored) ───────────
  /**
   * Returns a price discount for a single bulk purchase action.
   * Linear interpolation between bracket min and max, clamped at both ends.
   */
  def computeBulkOrderDiscount(model: Model, quantity: Int): Option[ModelDiscount] = {
    BULK_DISCOUNT_BRACKET.getOrElse(model.airplaneType, None).flatMap {
      case (minQty, minDiscount, maxQty, maxDiscount) =>
        if (quantity < minQty) None
        else {
          val t        = math.min(1.0, (quantity - minQty).toDouble / (maxQty - minQty))
          val discount = minDiscount + t * (maxDiscount - minDiscount)
          Some(ModelDiscount(model.id, discount, DiscountType.PRICE, DiscountReason.BULK_ORDER, None))
        }
    }
  }

  // ── Combined discount lookup ──────────────────────────────────────────────
  /**
   * All applicable discounts for a given airline + model, excluding bulk
   * (bulk is computed separately at order time since it requires quantity).
   */
  def getCombinedDiscountsByModelId(airlineId: Int, modelId: Int, currentCycle: Int): List[ModelDiscount] = {
    val discounts = ListBuffer[ModelDiscount]()
    // Blanket model discounts (e.g. LOW_DEMAND, stored in DB via AirplaneModelDiscountCache)
    discounts.appendAll(getBlanketModelDiscounts(modelId))
    // Obsolescence (computed)
    AirplaneModelCache.getModel(modelId).foreach { model =>
      computeObsolescenceDiscount(model, currentCycle).foreach(discounts.append(_))
    }
    discounts.toList
  }

  /**
   * All discounts for all models for a given airline — used for the model
   * browser display. Bulk discount not included (requires quantity).
   */
  def getAllCombinedDiscountsByAirlineId(airlineId: Int, currentCycle: Int): Map[Int, List[ModelDiscount]] = {
    AirplaneModelCache.allModels.values.map { model =>
      val discounts = ListBuffer[ModelDiscount]()
      discounts.appendAll(getBlanketModelDiscounts(model.id))
      computeObsolescenceDiscount(model, currentCycle).foreach(discounts.append(_))
      (model.id, discounts.toList)
    }.toMap
  }

  def getBlanketModelDiscounts(modelId: Int): List[ModelDiscount] = {
    AirplaneModelDiscountCache.getModelDiscount(modelId)
  }
}

// ── Enumerations ─────────────────────────────────────────────────────────────
object DiscountReason extends Enumeration {
  type Type = Value
  val OBSOLESCENCE, LOW_DEMAND, BULK_ORDER = Value
}

object DiscountType extends Enumeration {
  type Type = Value
  val PRICE, CONSTRUCTION_TIME = Value

  val description: DiscountType.Value => String = {
    case PRICE             => "Price"
    case CONSTRUCTION_TIME => "Construction Time"
    case _                 => "Unknown"
  }
}