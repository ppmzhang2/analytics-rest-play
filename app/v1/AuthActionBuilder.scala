package v1

import javax.inject.Inject
import net.logstash.logback.marker.LogstashMarker
import play.api.http.{FileMimeTypes, HttpVerbs}
import play.api.i18n.{Langs, MessagesApi}
import play.api.mvc.Results._
import play.api.mvc._
import play.api.{Logger, MarkerContext}

import scala.concurrent.{ExecutionContext, Future}

/**
  * A wrapped request for post resources.
  *
  * This is commonly used to hold request-specific information like
  * security credentials, and useful shortcut methods.
  */
trait AuthRequestHeader
  extends MessagesRequestHeader
    with PreferredMessagesProvider

class AuthRequest[A](request: Request[A], val messagesApi: MessagesApi)
  extends WrappedRequest(request)
    with AuthRequestHeader

/**
  * Provides an implicit marker that will show the request in all logger statements.
  */
trait RequestMarkerContext {

  import net.logstash.logback.marker.Markers

  private def marker(tuple: (String, Any)) = Markers.append(tuple._1, tuple._2)

  private implicit class RichLogstashMarker(marker1: LogstashMarker) {
    def &&(marker2: LogstashMarker): LogstashMarker = marker1.and(marker2)
  }

  implicit def requestHeaderToMarkerContext(
                                             implicit request: RequestHeader): MarkerContext = {
    MarkerContext {
      marker("id" -> request.id) && marker("host" -> request.host) && marker(
        "remoteAddress" -> request.remoteAddress)
    }
  }

}

/**
  * The action builder for the Post resource.
  *
  * This is the place to put logging, metrics, to augment
  * the request with contextual data, and manipulate the
  * result.
  */
class AuthActionBuilder @Inject()(messagesApi: MessagesApi,
                                  jwt: AppJwt,
                                  playBodyParsers: PlayBodyParsers)(
                                 implicit val executionContext: ExecutionContext)
  extends ActionBuilder[AuthRequest, AnyContent]
    with RequestMarkerContext
    with HttpVerbs {

  type AuthRequestBlock[A] = AuthRequest[A] => Future[Result]
  override val parser: BodyParser[AnyContent] = playBodyParsers.anyContent
  private val logger = Logger(this.getClass)

  override def invokeBlock[A](request: Request[A],
                              block: AuthRequestBlock[A]): Future[Result] = {
    // Convert to marker context and use request in block
    implicit val markerContext: MarkerContext = requestHeaderToMarkerContext(
      request)
    logger.trace(s"invokeBlock: ")

    val tokenOption: Option[String] = request.headers.get("Token")
    val authorized = tokenOption.exists(jwt.isValid)

    if (!authorized) {
      Future.successful(Forbidden("Dude, you’re not logged in."))
    } else {
      val usrId = tokenOption match {
        case Some(token) => jwt.usrIdOrElse(token, default = -1L)
        case _ => -1L
      }

      val future = block(new AuthRequest(
        request.withHeaders(request.headers.add("USRID" -> usrId.toString)),
        messagesApi))

      future.map { result =>
        request.method match {
          case GET | HEAD =>
            result.withHeaders("Cache-Control" -> s"max-age: 100",
              "User-ID" -> usrId.toString)
          case _ =>
            result.withHeaders("User-ID" -> usrId.toString)
        }
      }
    }

  }
}

/**
  * Packages up the component dependencies for the post controller.
  *
  * This is a good way to minimize the surface area exposed to the controller, so the
  * controller only has to have one thing injected.
  */
case class AuthControllerComponents @Inject()(
                                               authActionBuilder: AuthActionBuilder,
                                               actionBuilder: DefaultActionBuilder,
                                               parsers: PlayBodyParsers,
                                               messagesApi: MessagesApi,
                                               langs: Langs,
                                               fileMimeTypes: FileMimeTypes,
                                               executionContext: scala.concurrent.ExecutionContext)
  extends ControllerComponents

/**
  * Exposes actions and handler to the PostController by wiring the injected state into the base class.
  */
class AuthBaseController @Inject()(acc: AuthControllerComponents)
  extends BaseController
    with RequestMarkerContext {
  def AuthAction: AuthActionBuilder = acc.authActionBuilder

  override protected def controllerComponents: ControllerComponents = acc
}
