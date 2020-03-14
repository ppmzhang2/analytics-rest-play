package v1.models

import play.api.libs.json.{Json, OFormat}

case class PivotExpr(column: String, values: Seq[String])

object PivotExpr {
  implicit val format: OFormat[PivotExpr] = Json.format[PivotExpr]
}
