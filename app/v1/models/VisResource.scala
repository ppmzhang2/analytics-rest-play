package v1.models

import play.api.libs.json.{JsValue, Json, OFormat}

case class VisResource(data: Array[JsValue], chartOption: JsValue)

object VisResource {
  implicit val format: OFormat[VisResource] = Json.format[VisResource]
}
