package v1.auth

import javax.inject.{Inject, Provider}
import javax.naming.directory.InitialDirContext
import play.api.Logger
import play.api.data.Form
import play.api.libs.json.Json
import play.api.mvc._
import v1.models.DbRepository
import v1.{AppJwt, LdapHandler}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

case class AuthFormInput(username: String, password: String)

/**
  * Takes HTTP requests and produces JSON.
  */
class AuthController @Inject()(repo: DbRepository,
                               cc: MessagesControllerComponents,
                               ldap: LdapHandler,
                               jwt: AppJwt,
                               routerProvider: Provider[AuthRouter])(
                                implicit ec: ExecutionContext)
  extends MessagesAbstractController(cc) {
  private val demoUser = v1.DemoUser
  private val demoUserId = v1.DemoUserID

  private val logger = Logger(getClass)

  private val form: Form[AuthFormInput] = {
    import play.api.data.Forms._

    Form(
      mapping(
        "username" -> nonEmptyText
          .verifying("too few chars", s => lengthIsGreaterThanNCharacters(s, 2))
          .verifying("too many chars", s => lengthIsLessThanNCharacters(s, 20)),
        "password" -> nonEmptyText
          .verifying("too few chars", s => lengthIsGreaterThanNCharacters(s, 2))
          .verifying("too many chars", s => lengthIsLessThanNCharacters(s, 30))
      )(AuthFormInput.apply)(AuthFormInput.unapply)
    )
  }

  def processLogin: Action[AnyContent] = Action.async { implicit request =>
    def error(badForm: Form[AuthFormInput]): Future[Result] = {
      logger.trace(message = "bad login form")
      Future.successful(BadRequest(badForm.errorsAsJson))
    }

    def success(input: AuthFormInput): Future[Result] = {
      if (input.username == demoUser) Future.successful {
        logger.trace(message = s"demo user ${input.username} login")
        Ok(Json.toJson(
          Map("token" -> jwt.createToken(demoUserId))))
      } else if (!ldap.validate(input.username, input.password)) Future.successful {
        logger.trace(message = s"incorrect credential for user ${input.username}")
        Ok(Json.toJson(Map("token" -> "")))
      } else {
        repo.lookupUserNt(input.username).flatMap {
          case Some(user) =>
            logger.trace(message = s"old user ${input.username} login")
            Future.successful(Ok(Json.toJson(
              Map("token" -> jwt.createToken(user.id)))))
          case _ =>
            ldap.getContext(input.username, input.password) match {
              case Success(ctx) =>
                addNewUser(ctx, input.username).map { usrId =>
                  logger.trace(message = s"new user ${input.username} login")
                  Ok(Json.toJson(
                    Map("token" -> jwt.createToken(usrId))))
                }
              case Failure(exception) => Future.failed(exception)
            }
        }
      }

    }

    form.bindFromRequest().fold(error, success)
  }

  private def addNewUser(ctx: InitialDirContext, ntid: String): Future[Long] = {
    Try {
      val name = ldap.getName(ctx, ntid)
      repo.addUser(ntid, name, "")
    } match {
      case Success(i) => i
      case Failure(e) => Future.failed(e)
    }
  }

  private def lengthIsGreaterThanNCharacters(s: String, n: Int): Boolean = {
    if (s.length > n) true else false
  }

  private def lengthIsLessThanNCharacters(s: String, n: Int): Boolean = {
    if (s.length < n) true else false
  }

}
