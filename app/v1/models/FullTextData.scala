package v1.models

import play.api.libs.json.{Json, OFormat}

case class FullTextData(id: Long, score: Double)

object FullTextData {
  implicit val format: OFormat[FullTextData] = Json.format[FullTextData]
}
