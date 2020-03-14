package v1.models

import play.api.libs.json.{Json, OFormat}

case class User(id: Long, ntid: String, name: String, title: String)

object User {
  implicit val format: OFormat[User] = Json.format[User]
}
