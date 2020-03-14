package v1.models

import play.api.libs.json.{Json, OFormat}

case class GroupExpr(fields: Seq[String], alias: String)

object GroupExpr {
  implicit val format: OFormat[GroupExpr] = Json.format[GroupExpr]
}
