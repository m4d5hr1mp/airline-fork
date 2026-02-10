package com.patson.model.oil

case class OilPrice(price: Double, cycle: Int)
// Oil Price Values Used:
// 1. Pre V0.1 (Local Dev) -- 0.684
// 2. V0.1 Production -- 0.725
// 3. V0.1 Production, Patched -- 1.75
// 4. V0.2 Production -- ???


object OilPrice {
  val DEFAULT_UNIT_COST = 1.75 //This is cost per 1 KG of Jet-A! Not per Liter! (And at 70$ Oil price exactly)
  val DEFAULT_PRICE: Double = 70
  val unitCost: (Double => Double) = (price: Double) => price / DEFAULT_PRICE * DEFAULT_UNIT_COST
}