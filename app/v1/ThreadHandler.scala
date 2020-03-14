import java.util.concurrent.TimeUnit

import javax.inject.{Inject, Named}
import akka.actor.ActorRef
import akka.actor.ActorSystem
import play.api.Logger
import play.api.inject.SimpleModule
import play.api.inject.bind

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class MyActorTask @Inject() (actorSystem: ActorSystem, @Named("some-actor") someActor: ActorRef)(
  implicit executionContext: ExecutionContext
) {
  private val logger = Logger(getClass)

  private def initialize(): Unit = {
    actorSystem.scheduler.schedule(initialDelay = Duration(1, TimeUnit.SECONDS),
      interval = Duration(10, TimeUnit.SECONDS))({ () =>
      actorSystem.log.info("Executing something...")
      logger.trace(message = s"scheduling ...")
    })(executionContext) // using the custom execution context
  }

  initialize()
}

class TasksModule extends SimpleModule(bind[MyActorTask].toSelf.eagerly())
