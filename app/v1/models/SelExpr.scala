package v1.models

import play.api.libs.json.{Json, OFormat}

case class SelExpr(expr: Seq[ArithExpr], alias: String)

object SelExpr {
  implicit val format: OFormat[SelExpr] = Json.format[SelExpr]
}
