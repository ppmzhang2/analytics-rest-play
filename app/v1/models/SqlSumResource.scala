package v1.models

import java.time.LocalDate

import play.api.libs.json.{Json, OFormat}

case class SqlSumResource(id: Long, usrid: Long, ntid: String, desc: String, stars: Int, updated: LocalDate)

object SqlSumResource {
  implicit val format: OFormat[SqlSumResource] = Json.format[SqlSumResource]
}
