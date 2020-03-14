package v1.models

import play.api.libs.json.{Json, OFormat}

case class DataOption(filters: Seq[FilterExpr], pivotExpr: PivotExpr,
                      groupExprs: Seq[GroupExpr], aggExprs: Seq[AggExpr],
                      selExprs: Seq[SelExpr])

object DataOption {
  implicit val format: OFormat[DataOption] = Json.format[DataOption]
}
