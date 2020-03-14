package v1.models

import play.api.libs.json.{Json, OFormat}

case class Star(sqlid: Long, usrid: Long)

object Star {
  implicit val format: OFormat[Star] = Json.format[Star]
}
