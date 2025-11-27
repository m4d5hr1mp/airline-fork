package com.patson.model
case class LinkClassValues(economyVal : Int, businessVal : Int, firstVal : Int) extends AbstractLinkClassValues(economyVal, businessVal, firstVal) {
  override def toString() = {
    s"$economyVal / $businessVal / $firstVal"
  }
}

abstract class AbstractLinkClassValues(economyVal : Int, businessVal : Int, firstVal : Int) {
  def apply(linkClass : LinkClass) = {
    linkClass match {
      case ECONOMY => economyVal
      case BUSINESS => businessVal
      case FIRST => firstVal
    }
  }

  val total = economyVal + businessVal + firstVal

  def +(otherValue : LinkClassValues) : LinkClassValues = {
    LinkClassValues(economyVal + otherValue.economyVal, businessVal + otherValue.businessVal, firstVal + otherValue.firstVal)
  }

  def -(otherValue : LinkClassValues) : LinkClassValues = {
    LinkClassValues(economyVal - otherValue.economyVal, businessVal - otherValue.businessVal, firstVal - otherValue.firstVal)
  }

  def *(otherValue : LinkClassValues) : LinkClassValues = {
    LinkClassValues(economyVal * otherValue.economyVal, businessVal * otherValue.businessVal, firstVal * otherValue.firstVal)
  }

  def *(multiplier : Double) : LinkClassValues = {
    LinkClassValues((economyVal * multiplier).toInt, (businessVal * multiplier).toInt, (firstVal * multiplier).toInt)
  }

  def /(divider : Int) : LinkClassValues = {
    LinkClassValues(economyVal / divider, businessVal / divider, firstVal / divider)
  }
}

object LinkClassValues {
  def getInstance(economy : Int = 0, business : Int = 0, first : Int = 0) : LinkClassValues = {
    LinkClassValues(economy, business, first)
  }
  def getInstanceByMap(map : Map[LinkClass, Int]) : LinkClassValues = {
    LinkClassValues(map.getOrElse(ECONOMY, 0), map.getOrElse(BUSINESS, 0), map.getOrElse(FIRST, 0))
  }
}

/* FULL REFACTORED VERSION FOR INTRODUCTION OF PREMIUM ECONOMY:

package com.patson.model

case class LinkClassValues(economyVal: Int, premiumEconomyVal: Int, businessVal: Int, firstVal: Int)
  extends AbstractLinkClassValues(economyVal, premiumEconomyVal, businessVal, firstVal) {
  
  override def toString(): String = {
    s"$economyVal / $premiumEconomyVal / $businessVal / $firstVal"
  }
}

abstract class AbstractLinkClassValues(economyVal: Int, premiumEconomyVal: Int, businessVal: Int, firstVal: Int) {
  
  def apply(linkClass: LinkClass): Int = {
    linkClass match {
      case ECONOMY => economyVal
      case PREMIUM_ECONOMY => premiumEconomyVal
      case BUSINESS => businessVal
      case FIRST => firstVal
    }
  }
  
  val total: Int = economyVal + premiumEconomyVal + businessVal + firstVal
  
  def +(otherValue: LinkClassValues): LinkClassValues = {
    LinkClassValues(
      economyVal + otherValue.economyVal,
      premiumEconomyVal + otherValue.premiumEconomyVal,
      businessVal + otherValue.businessVal,
      firstVal + otherValue.firstVal
    )
  }
  
  def -(otherValue: LinkClassValues): LinkClassValues = {
    LinkClassValues(
      economyVal - otherValue.economyVal,
      premiumEconomyVal - otherValue.premiumEconomyVal,
      businessVal - otherValue.businessVal,
      firstVal - otherValue.firstVal
    )
  }
  
  def *(otherValue: LinkClassValues): LinkClassValues = {
    LinkClassValues(
      economyVal * otherValue.economyVal,
      premiumEconomyVal * otherValue.premiumEconomyVal,
      businessVal * otherValue.businessVal,
      firstVal * otherValue.firstVal
    )
  }
  
  def *(multiplier: Double): LinkClassValues = {
    LinkClassValues(
      (economyVal * multiplier).toInt,
      (premiumEconomyVal * multiplier).toInt,
      (businessVal * multiplier).toInt,
      (firstVal * multiplier).toInt
    )
  }
  
  def /(divider: Int): LinkClassValues = {
    LinkClassValues(
      economyVal / divider,
      premiumEconomyVal / divider,
      businessVal / divider,
      firstVal / divider
    )
  }
}

object LinkClassValues {
  
  def getInstance(economy: Int = 0, premiumEconomy: Int = 0, business: Int = 0, first: Int = 0): LinkClassValues = {
    LinkClassValues(economy, premiumEconomy, business, first)
  }
  
  def getInstanceByMap(map: Map[LinkClass, Int]): LinkClassValues = {
    LinkClassValues(
      map.getOrElse(ECONOMY, 0),
      map.getOrElse(PREMIUM_ECONOMY, 0),
      map.getOrElse(BUSINESS, 0),
      map.getOrElse(FIRST, 0)
    )
  }
}

 */