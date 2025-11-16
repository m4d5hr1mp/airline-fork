error id: file://<WORKSPACE>/airline-data/src/main/scala/com/patson/model/Loan.scala:scala/Int#
file://<WORKSPACE>/airline-data/src/main/scala/com/patson/model/Loan.scala
empty definition using pc, found symbol in pc: scala/Int#
empty definition using semanticdb
empty definition using fallback
non-local guesses:
	 -Int#
	 -scala/Predef.Int#
offset: 1419
uri: file://<WORKSPACE>/airline-data/src/main/scala/com/patson/model/Loan.scala
text:
```scala
package com.patson.model

case class Loan(airlineId : Int, principal : Long, annualRate : BigDecimal, creationCycle : Int, lastPaymentCycle : Int, term : Int, var id : Int = 0) extends IdObject {
  val WEEKS_PER_YEAR = 52
  val EARLY_PAYMENT_FEE_RATE = 0.01
  //Payment = P x (r / n) x (1 + r / n)^n(t)] / ((1 + r / n)^n(t) - 1)
  val weeklyRate = annualRate.toDouble / WEEKS_PER_YEAR
  val weeklyPayment : Long = Math.ceil(principal * weeklyRate * Math.pow(1 + weeklyRate, term) / (Math.pow(1 + weeklyRate, term) - 1)).toLong
  val interest = weeklyPayment * term - principal
  val total = principal + interest

  val remainingTerm = (currentCycle : Int) => creationCycle + term - currentCycle
  val remainingPayment : (Int => Long)= (currentCycle : Int) => (total - (term - remainingTerm(currentCycle)) * weeklyPayment).toLong
//  val remainingInterest = (currentCycle : Int) => interestWeeklyPayment(currentCycle) * remainingTerm
  val remainingPrincipal : (Int => Long) = (currentCycle : Int) => {
    val paidMonth = currentCycle - creationCycle
    (principal * Math.pow(1 + weeklyRate, paidMonth) - weeklyPayment * (Math.pow(1 + weeklyRate, paidMonth) - 1) / weeklyRate).toLong
  }

  val weeklyInterest : (Int => Long) = (currentCycle : Int) => {
    if (currentCycle <= creationCycle) {
      0
    } else {
      (remainingPrincipal(currentCycle - 1) * weeklyRate).toLong
    }
  }

  val weeklyPrincipal : (I@@nt => Long) = (currentCycle : Int) => {
    if (currentCycle <= creationCycle) {
      0
    } else {
      weeklyPayment - weeklyInterest(currentCycle)
    }
  }


  val earlyRepaymentFee : (Int => Long) = (currentCycle : Int) => ((remainingPayment(currentCycle) - remainingPrincipal(currentCycle)) * 0.5).toLong //half of the remaining interest
  val earlyRepayment : (Int => Long) = (currentCycle : Int) => remainingPrincipal(currentCycle) + earlyRepaymentFee(currentCycle)



}



```


#### Short summary: 

empty definition using pc, found symbol in pc: scala/Int#