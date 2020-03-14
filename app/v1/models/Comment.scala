package v1.models

import java.time.LocalDate

import play.api.libs.json.{Json, OFormat}

case class Comment(id: Long, reid: Long, usrid: Long, reusrid: Long, sqlid: Long, content: String, created: LocalDate)

object Comment {
  implicit val format: OFormat[Comment] = Json.format[Comment]
}
