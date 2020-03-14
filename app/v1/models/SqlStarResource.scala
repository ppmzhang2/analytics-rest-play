package v1.models

import play.api.libs.json.{Json, OFormat}

case class SqlStarResource(stars: Int)

object SqlStarResource {
  implicit val format: OFormat[SqlStarResource] = Json.format[SqlStarResource]
}
