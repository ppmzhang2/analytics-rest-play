package v1.models

import play.api.libs.json.{Json, OFormat}

case class FilterExpr(field: String, operator: String, operand: String)

object FilterExpr {
  implicit val format: OFormat[FilterExpr] = Json.format[FilterExpr]
}
