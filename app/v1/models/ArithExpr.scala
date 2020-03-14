package v1.models

import play.api.libs.json.{Json, OFormat}

case class ArithExpr(field: String, operator: String)

object ArithExpr {
  implicit val format: OFormat[ArithExpr] = Json.format[ArithExpr]
}
