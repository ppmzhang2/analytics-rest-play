package v1.models

import play.api.libs.json.{JsValue, Json, OFormat}

case class ReportResource(id: Long, sqlId: Long, usrId: Long, data: JsValue, chartOption: JsValue)

object ReportResource {
  implicit val format: OFormat[ReportResource] = Json.format[ReportResource]
}
