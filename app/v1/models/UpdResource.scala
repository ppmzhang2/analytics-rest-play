package v1.models

import play.api.libs.json.{Json, OFormat}

case class UpdResource(records: Int)

object UpdResource {
  implicit val format: OFormat[UpdResource] = Json.format[UpdResource]
}
