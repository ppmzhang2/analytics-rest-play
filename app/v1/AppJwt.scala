package v1

import java.time.Clock

import pdi.jwt._
import play.api.libs.json.{JsResult, JsSuccess, Json, OFormat}

import scala.util.{Success, Try}

class AppJwt {

  private val key = "this is a secret key"
  private val alg = JwtAlgorithm.HS256

  def createToken(usrId: Long): String = {
    implicit val clock: Clock = Clock.systemUTC
    val content = Seq("usrId" -> usrId)
    val jwtClaim = content.foldLeft(JwtClaim().startsNow.issuedNow) {
      (x1: JwtClaim, x2: (String, Long)) => x1 + (x2._1, x2._2)
    }
    JwtJson.encode(jwtClaim, key, alg)
  }

  def isValid(token: String): Boolean = {
    JwtJson.isValid(token, key, Seq(alg))
  }

  def usrIdOrElse(token: String, default: Long): Long = {
    extract(token).flatMap {
      case JsSuccess(value, _) => Success(value.usrId)
      case _ => Success(default)
    }.get
  }

  private def extract(token: String): Try[JsResult[Claim]] = {
    implicit val format: OFormat[Claim] = Json.format[Claim]

    JwtJson.decode(token, key, Seq(alg)).map { jwtClaim =>
      Json.fromJson[Claim](Json.parse(jwtClaim.toJson))
    }
  }

  private case class Claim(usrId: Long, iat: Long, nbf: Long)

}
