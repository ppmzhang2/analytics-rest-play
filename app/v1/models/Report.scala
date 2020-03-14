package v1.models

import play.api.libs.json.{Json, OFormat}

case class Report(id: Long, sqlid: Long, data: String, chart: String)

object Report {
  implicit val format: OFormat[Report] = Json.format[Report]
}
