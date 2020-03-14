package v1.auth

import javax.inject.Inject
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._

/**
  * Routes and URLs to the PostResource controller.
  */
class AuthRouter @Inject()(controller: AuthController) extends SimpleRouter {
  val prefix = "/v1/auth"

  override def routes: Routes = {
    case POST(p"/") =>
      controller.processLogin
  }
}
