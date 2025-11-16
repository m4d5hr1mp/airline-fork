package com.patson.model

case class Loan(airlineId: Int, principal: Long, annualRate: BigDecimal, creationCycle: Int, lastPaymentCycle: Int, term: Int, var id: Int = 0) extends IdObject {
  val WEEKS_PER_YEAR = 52
  val EARLY_PAYMENT_FEE_RATE = 0.01

  val weeklyRate: Double = annualRate.toDouble / WEEKS_PER_YEAR

  val weeklyPayment: Long = if (weeklyRate == 0) {
    // Special case for 0% interest: straight-line principal repayment
    (principal.toDouble / term).ceil.toLong
  } else {
    // Standard amortizing formula
    Math.ceil(principal * weeklyRate * Math.pow(1 + weeklyRate, term) / (Math.pow(1 + weeklyRate, term) - 1)).toLong
  }

  val interest: Long = weeklyPayment * term - principal
  val total: Long = principal + interest

  val remainingTerm: Int => Int = (currentCycle: Int) => creationCycle + term - currentCycle

  val remainingPayment: Int => Long = (currentCycle: Int) => (total - (term - remainingTerm(currentCycle)) * weeklyPayment).toLong

  val remainingPrincipal: Int => Long = (currentCycle: Int) => {
    if (weeklyRate == 0) {
      // For 0% interest: linear reduction
      weeklyPayment * remainingTerm(currentCycle)
    } else {
      val paidMonth = currentCycle - creationCycle
      (principal * Math.pow(1 + weeklyRate, paidMonth) - weeklyPayment * (Math.pow(1 + weeklyRate, paidMonth) - 1) / weeklyRate).toLong
    }
  }

  val weeklyInterest: Int => Long = (currentCycle: Int) => {
    if (currentCycle <= creationCycle || weeklyRate == 0) {
      0
    } else {
      (remainingPrincipal(currentCycle - 1) * weeklyRate).toLong
    }
  }

  val weeklyPrincipal: Int => Long = (currentCycle: Int) => {
    if (currentCycle <= creationCycle) {
      0
    } else {
      weeklyPayment - weeklyInterest(currentCycle)
    }
  }

  val earlyRepaymentFee: Int => Long = (currentCycle: Int) => ((remainingPayment(currentCycle) - remainingPrincipal(currentCycle)) * 0.5).toLong // Half of remaining interest

  val earlyRepayment: Int => Long = (currentCycle: Int) => remainingPrincipal(currentCycle) + earlyRepaymentFee(currentCycle)
}