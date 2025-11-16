package com.patson.model.oil

case class OilPrice(price: Double, cycle: Int)

object OilPrice {
  val DEFAULT_UNIT_COST = 0.684 //This is cost per 1 KG of Jet-A! Not per Liter! (And at 70$ Oil price exactly)
  val DEFAULT_PRICE: Double = 70
  // the price used for actual simulation calculation
  val unitCost: (Double => Double) = (price: Double) => price / DEFAULT_PRICE * DEFAULT_UNIT_COST
}