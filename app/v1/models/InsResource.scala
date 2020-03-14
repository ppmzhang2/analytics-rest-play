package v1.models

import play.api.libs.json.{Json, OFormat}

case class InsResource(record: Long)

object InsResource {
  implicit val format: OFormat[InsResource] = Json.format[InsResource]
}
