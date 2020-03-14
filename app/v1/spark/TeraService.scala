package v1.spark

import javax.inject.Singleton

import scala.sys.process._
import scala.util.{Failure, Success, Try}

@Singleton
class TeraService {
  private val exec = v1.TeraExecutorPath

  def teraExec(email: String, sqlStmt: String, log: String): Int = {
    Try(Seq(exec, email, sqlStmt, log).!) match {
      case Success(value) => value
      case Failure(_) => 999
    }
  }

}
