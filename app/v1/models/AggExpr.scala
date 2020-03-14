package v1.models

import play.api.libs.json.{Json, OFormat}

case class AggExpr(field: String, func: String)

object AggExpr {
  implicit val format: OFormat[AggExpr] = Json.format[AggExpr]
}
