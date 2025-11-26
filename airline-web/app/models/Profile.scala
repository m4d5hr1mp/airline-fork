package models
import com.patson.model.Loan
import com.patson.model.Airport
import com.patson.model.airplane.Airplane
case class Profile(name: String, description: String, cash: Long, airport: Airport, reputation: Int = 0, airplanes: List[Airplane] = List.empty, loans: List[Loan] = List.empty)