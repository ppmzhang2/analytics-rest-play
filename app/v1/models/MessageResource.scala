package v1.models

import play.api.libs.json.{Json, OFormat}

case class MessageResource(explain: String)

object MessageResource {
  implicit val format: OFormat[MessageResource] = Json.format[MessageResource]
}
