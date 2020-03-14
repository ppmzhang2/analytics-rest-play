package v1.models

import java.time.LocalDate

import play.api.libs.json.{Json, OFormat}

case class Sql(id: Long, usrid: Long, desc: String,
               content: String, stars: Int,
               execution: Int, exportation: Int,
               updated: LocalDate)

object Sql {
  implicit val format: OFormat[Sql] = Json.format[Sql]
}
