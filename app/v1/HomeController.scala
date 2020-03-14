package v1

import java.io.File
import java.nio.file.{Files, Paths}
import java.time.LocalDateTime

import javax.inject.{Inject, Provider}
import org.apache.commons.io.FileUtils
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.mvc._
import v1.models._
import v1.spark.{SparkService, TeraService}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}


/**
 * Takes HTTP requests and produces JSON.
 */
class HomeController @Inject()(repo: DbRepository,
                               teraService: TeraService,
                               sparkService: SparkService,
                               cc: AuthControllerComponents,
                               routerProvider: Provider[HomeRouter])(
                                implicit ec: ExecutionContext)
  extends AuthBaseController(cc) {

  private val logger = Logger(getClass)

  def aboutMe(): Action[AnyContent] = AuthAction.async { implicit request =>
    userResult(getUsrId(request))
  }

  private def userResult(usrId: Long): Future[Result] = {
    logger.trace(message = s"show user: $usrId")
    repo.lookupUser(usrId).map {
      case Some(user) => Ok(Json.toJson(user))
      case _ => NotFound(Json.toJson(MessageResource("no such user")))
    }
  }

  def lookupUser(usrId: Long): Action[AnyContent] = AuthAction.async { implicit request =>
    userResult(usrId)
  }

  def lookupSql(sqlId: Long): Action[AnyContent] = AuthAction.async { implicit request =>
    logger.trace(message = s"show sql: $sqlId")
    repo.lookupSql(sqlId).map {
      case Some(sql) =>
        Ok(Json.toJson(sql))
      case _ =>
        NotFound(Json.toJson(MessageResource("no such sql")))
    }
  }

  def allSql(): Action[AnyContent] = AuthAction.async { implicit request =>
    logger.trace(message = "fetching all SQLs")
    repo.allSql().map { sqlSeq =>
      Ok(Json.toJson(sqlSeq))
    }
  }

  def allSqlBy(usrId: Long): Action[AnyContent] = AuthAction.async { implicit request =>
    logger.trace(message = s"fetching SQLs by: $usrId")
    repo.allSqlBy(usrId).map { sqlSeq =>
      Ok(Json.toJson(sqlSeq))
    }
  }

  private def getUsrId[A](request: AuthRequest[A]): Long = {
    request.headers.get("USRID").getOrElse("-1").toLong
  }

  private def getUsrEmail[A](request: AuthRequest[A]): Future[String] = {
    repo.lookupUser(getUsrId(request)).flatMap {
      case Some(user) => Future(user.ntid + v1.EmailSuffix)
      case _ => Future.failed(new Exception("no such user"))
    }
  }

  def updateStar(): Action[AnyContent] = AuthAction.async { implicit request =>
    case class FormInput(sqlId: Long)

    val form: Form[FormInput] =
      Form(
        mapping(
          "sqlId" -> longNumber
        )(FormInput.apply)(FormInput.unapply)
      )

    val usrId: Long = getUsrId(request)

    def failure(badForm: Form[FormInput]): Future[Result] = {
      Future.successful(BadRequest(badForm.errorsAsJson))
    }

    def success(input: FormInput): Future[Result] = {
      logger.trace(message = s"update stars for SQL ${input.sqlId} ...")

      repo.updateStar(input.sqlId, usrId).map {
        case Some(stars) =>
          Created(Json.toJson(SqlStarResource(stars)))
        case _ => UnprocessableEntity(Json.toJson(
          MessageResource("currently cannot (un)star this sql")))
      }
    }

    form.bindFromRequest().fold(failure, success)
  }

  def editSql(): Action[AnyContent] = AuthAction.async { implicit request =>
    case class FormInput(id: Long, desc: String, content: String)

    val form: Form[FormInput] =
      Form(
        mapping(
          "id" -> longNumber,
          "desc" -> nonEmptyText,
          "content" -> nonEmptyText
        )(FormInput.apply)(FormInput.unapply)
      )

    def failure(badForm: Form[FormInput]): Future[Result] = {
      Future.successful(BadRequest(badForm.errorsAsJson))
    }

    def success(input: FormInput): Future[Result] = {
      logger.trace(message = s"editing SQL ${input.id} ...")

      repo.editSql(input.id, input.desc, input.content).map {
        case 0 =>
          UnprocessableEntity(Json.toJson(
            MessageResource("currently cannot edit this sql")))
        case i =>
          Created(Json.toJson(UpdResource(i)))
      }
    }

    form.bindFromRequest().fold(failure, success)
  }

  def addSql(): Action[AnyContent] = AuthAction.async { implicit request =>
    case class SqlFormInput(desc: String, content: String)

    val form: Form[SqlFormInput] =
      Form(
        mapping(
          "desc" -> nonEmptyText,
          "content" -> nonEmptyText
        )(SqlFormInput.apply)(SqlFormInput.unapply)
      )

    val usrId: Long = getUsrId(request)

    def failure(badForm: Form[SqlFormInput]): Future[Result] = {
      Future.successful(BadRequest(badForm.errorsAsJson))
    }

    def success(input: SqlFormInput): Future[Result] = {
      logger.trace(message = "adding SQL ...")

      repo.addSql(usrId, input.desc, input.content).map { id =>
        Created(Json.toJson(InsResource(id)))
      }
    }

    form.bindFromRequest().fold(failure, success)
  }

  def forkSql(sqlId: Long): Action[AnyContent] = AuthAction.async { implicit request =>
    val usrId: Long = getUsrId(request)

    repo.forkSql(sqlId, usrId).map {
      case Some(id) => Ok(Json.toJson(InsResource(id)))
      case _ => UnprocessableEntity(Json.toJson(MessageResource("currently cannot fork")))
    }
  }

  def delSql(): Action[AnyContent] = AuthAction.async { implicit request =>
    case class FormInput(id: Long)

    val form: Form[FormInput] =
      Form(
        mapping(
          "id" -> longNumber
        )(FormInput.apply)(FormInput.unapply)
      )

    def failure(badForm: Form[FormInput]): Future[Result] = {
      Future.successful(BadRequest(badForm.errorsAsJson))
    }

    def success(input: FormInput): Future[Result] = {
      logger.trace(message = s"deleting SQL ${input.id} ...")

      repo.delSql(input.id).map {
        case 0 =>
          NotFound(Json.toJson(MessageResource(s"SQL ${input.id} not found")))
        case i =>
          Created(Json.toJson(UpdResource(i)))
      }
    }

    form.bindFromRequest().fold(failure, success)
  }

  def lookupReportResource(id: Long): Action[AnyContent] = AuthAction.async { implicit request =>
    logger.trace(message = s"Querying options for report: $id ...")

    repo.lookupReportResource(id).map {
      case Some(report) =>
        Ok(Json.toJson(report))
      case _ => UnprocessableEntity(
        Json.toJson(MessageResource("Currently no report option available!")))
    }
  }

  def addReport(): Action[AnyContent] = AuthAction.async { implicit request =>
    case class FormInput(sqlId: Long, dataOption: String, chartOption: String)

    val form: Form[FormInput] =
      Form(
        mapping(
          "sqlId" -> longNumber,
          "dataOption" -> nonEmptyText,
          "chartOption" -> nonEmptyText
        )(FormInput.apply)(FormInput.unapply)
      )

    def failure(badForm: Form[FormInput]): Future[Result] = {
      Future.successful(BadRequest(badForm.errorsAsJson))
    }

    def success(input: FormInput): Future[Result] = {
      logger.trace(message = s"adding report for sql ${input.sqlId} ...")

      repo.addReport(input.sqlId, input.dataOption, input.chartOption).map { id =>
        Created(Json.toJson(InsResource(id)))
      }
    }

    form.bindFromRequest().fold(failure, success)
  }

  def forkReport(): Action[AnyContent] = AuthAction.async { implicit request =>
    case class FormInput(rptId: Long, sqlId: Long)

    val form: Form[FormInput] =
      Form(
        mapping(
          "rptId" -> longNumber,
          "sqlId" -> longNumber
        )(FormInput.apply)(FormInput.unapply)
      )

    def failure(badForm: Form[FormInput]): Future[Result] = {
      Future.successful(BadRequest(badForm.errorsAsJson))
    }

    def success(input: FormInput): Future[Result] = {
      logger.trace(message = s"forking report ${input.rptId} to SQL ${input.sqlId} ...")

      repo.forkReport(input.rptId, input.sqlId).map {
        case Some(i) =>
          Created(Json.toJson(InsResource(i)))
        case _ =>
          UnprocessableEntity(Json.toJson(
            MessageResource("currently cannot fork this report")))
      }
    }

    form.bindFromRequest().fold(failure, success)
  }

  def allReportResourceBySql(sqlId: Long): Action[AnyContent] = AuthAction.async { implicit request =>
    logger.trace(message = s"Querying report options of SQL $sqlId ...")

    repo.allReportResourceBySql(sqlId).map { reportOption =>
      Ok(Json.toJson(reportOption))
    }
  }

  def allVisResourceBySql(sqlId: Long): Action[AnyContent] = AuthAction.async { implicit request =>
    logger.trace(message = s"Querying report of SQL $sqlId ...")

    repo.allReportResourceBySql(sqlId).map { s =>
      s.map { report =>
        VisResource(
          Json.fromJson[DataOption](report.data).map { dataOption =>
            sparkService.grouped(parquetPath(sqlId),
              dataOption.filters,
              dataOption.groupExprs,
              dataOption.pivotExpr,
              dataOption.aggExprs,
              dataOption.selExprs)
              .map(Json.parse)
          }.get,
          report.chartOption)
      }
    }.map { s =>
      Ok(Json.toJson(s))
    }

  }

  def editReport(): Action[AnyContent] = AuthAction.async { implicit request =>
    case class FormInput(rptId: Long, dataOption: String, chartOption: String)

    val form: Form[FormInput] =
      Form(
        mapping(
          "rptId" -> longNumber,
          "dataOption" -> nonEmptyText,
          "chartOption" -> nonEmptyText
        )(FormInput.apply)(FormInput.unapply)
      )

    def failure(badForm: Form[FormInput]): Future[Result] = {
      Future.successful(BadRequest(badForm.errorsAsJson))
    }

    def success(input: FormInput): Future[Result] = {
      logger.trace(message = s"updating options for report ${input.rptId} ...")

      repo.editReport(input.rptId, input.dataOption, input.chartOption).map {
        case 0 =>
          UnprocessableEntity(Json.toJson(
            MessageResource("currently cannot edit the report option")))
        case i =>
          Created(Json.toJson(UpdResource(i)))
      }
    }

    form.bindFromRequest().fold(failure, success)
  }

  def delReport(): Action[AnyContent] = AuthAction.async { implicit request =>
    case class FormInput(id: Long)

    val form: Form[FormInput] =
      Form(
        mapping(
          "id" -> longNumber
        )(FormInput.apply)(FormInput.unapply)
      )

    def failure(badForm: Form[FormInput]): Future[Result] = {
      Future.successful(BadRequest(badForm.errorsAsJson))
    }

    def success(input: FormInput): Future[Result] = {
      logger.trace(message = s"deleting report option ${input.id} ...")

      repo.delReport(input.id).map {
        case 0 =>
          UnprocessableEntity(Json.toJson(
            MessageResource("currently cannot delete the report option")))
        case i =>
          Created(Json.toJson(UpdResource(i)))
      }
    }

    form.bindFromRequest().fold(failure, success)
  }

  def allCommentsBySql(sqlId: Long): Action[AnyContent] = AuthAction.async { implicit request =>
    logger.trace(message = s"comments on $sqlId")
    repo.allCommentsBySql(sqlId).map { commentSeq =>
      Ok(Json.toJson(commentSeq))
    }
  }

  def addComment(): Action[AnyContent] = AuthAction.async { implicit request =>
    case class FormInput(sqlId: Long, content: String, reId: Long)

    val form: Form[FormInput] =
      Form(
        mapping(
          "sqlId" -> longNumber,
          "content" -> nonEmptyText,
          "reId" -> longNumber
        )(FormInput.apply)(FormInput.unapply)
      )

    val usrId: Long = getUsrId(request)

    def failure(badForm: Form[FormInput]): Future[Result] = {
      Future.successful(BadRequest(badForm.errorsAsJson))
    }

    def success(input: FormInput): Future[Result] = {
      logger.trace(message = s"adding Comment on ${input.sqlId} ...")

      repo.addCommont(usrId, input.sqlId, input.content, Some(input.reId)).map { id =>
        Created(Json.toJson(InsResource(id)))
      }
    }

    form.bindFromRequest().fold(failure, success)
  }

  def tdExec(): Action[AnyContent] = AuthAction.async { implicit request =>
    case class FormInput(id: Long)

    val form: Form[FormInput] =
      Form(
        mapping(
          "id" -> longNumber
        )(FormInput.apply)(FormInput.unapply)
      )

    def failure(badForm: Form[FormInput]): Future[Result] = {
      Future.successful(BadRequest(badForm.errorsAsJson))
    }

    def success(input: FormInput): Future[Result] = {
      logger.trace(message = s"executing Teradata SQLs ...")
      val dateTime = LocalDateTime.now().toString.replace("-", "")
        .replace(":", "").replace(".", "")

      getUsrEmail(request).flatMap { email =>
        repo.lookupSql(input.id).map {
          case Some(sql) =>
            if (sql.execution >= 0) {
              repo.updateSqlExecStatus(input.id, execStatus = -1).map { _ =>
                teraService.teraExec(email, sql.content,
                  log = "easy_" + dateTime + s"_${sql.id}.log")
              }.flatMap { rc =>
                repo.updateSqlExecStatus(input.id, execStatus = rc)
              }.flatMap { _ =>
                repo.lookupSql(input.id).map { op =>
                  op.map(sql => sql.execution).get
                }
              }.onComplete {
                case Success(0) =>
                  logger.trace(message = s"successfully executed SQL ${input.id}")
                case Success(_) =>
                  logger.warn(message = s"SQL ${input.id} execution failed")
                case Failure(exception) =>
                  logger.warn(message = exception.toString)
              }

              Created(Json.toJson(MessageResource("Executing ...")))
            }
            else {
              UnprocessableEntity(Json.toJson(
                MessageResource("Still executing, please wait ...")))
            }
          case _ => NotFound(Json.toJson(MessageResource("no such sql")))
        }
      }
    }

    form.bindFromRequest().fold(failure, success)
  }

  def tdToSpark(): Action[AnyContent] = AuthAction.async { implicit request =>
    case class FormInput(table: String, id: Long)

    val form: Form[FormInput] =
      Form(
        mapping(
          "table" -> nonEmptyText,
          "id" -> longNumber
        )(FormInput.apply)(FormInput.unapply)
      )

    def failure(badForm: Form[FormInput]): Future[Result] = {
      Future.successful(BadRequest(badForm.errorsAsJson))
    }

    def success(input: FormInput): Future[Result] = {
      val maxRow = 500000

      logger.trace(message = "Exporting to parquet ...")

      repo.lookupSql(input.id).map {
        case Some(sql) =>
          if (sql.exportation >= 0) {
            repo.updateSqlExpStatus(input.id, expStatus = -1).map { _ =>
              sparkService.count(input.table)
            }.flatMap { rowCount =>
              if (rowCount > maxRow) {
                Future.failed(new Exception("too many rows to import"))
              } else {
                Future {
                  FileUtils.deleteQuietly(new File(parquetPath(input.id)))
                }.map { _ =>
                  sparkService.teraToParquet(input.table, parquetPath(input.id))
                }
              }
            }.onComplete {
              case Success(path) =>
                repo.updateSqlExpStatus(input.id, expStatus = 0)
                logger.trace(message = s"successfully exported to $path")
              case Failure(exception) =>
                repo.updateSqlExpStatus(input.id, expStatus = 0)
                logger.warn(message = exception.toString)
            }
            Created(Json.toJson(MessageResource("Executing ...")))
          }
          else {
            UnprocessableEntity(Json.toJson(
              MessageResource("Still executing, please wait ...")))
          }
        case _ => NotFound(Json.toJson(MessageResource("no such sql")))
      }

    }

    form.bindFromRequest().fold(failure, success)
  }

  private def parquetPath(id: Long): String = {
    v1.ParquetPath + s"/rpt_$id"
  }

  private def sparkValidCall(sqlId: Long, result: => Result): Future[Result] = {
    repo.lookupSql(sqlId).map {
      case Some(sql) =>
        if (sql.exportation < 0) {
          Locked(Json.toJson(MessageResource("Still executing, please wait ...")))
        } else if (!Files.exists(Paths.get(parquetPath(sqlId)))) {
          FailedDependency(Json.toJson(MessageResource("data not loaded yet")))
        } else {
          result
        }
      case _ => NotFound(Json.toJson(MessageResource("no such sql")))
    }
  }

  def sparkValidate(sqlId: Long): Action[AnyContent] = AuthAction.async { implicit request =>
    sparkValidCall(sqlId, Ok(Json.toJson(MessageResource("data available"))))
  }

  def sparkUnique(): Action[AnyContent] = AuthAction.async { implicit request =>
    case class FormInput(sqlId: Long, column: String, n: Int)

    val form: Form[FormInput] =
      Form(
        mapping(
          "sqlId" -> longNumber,
          "column" -> nonEmptyText,
          "n" -> number
        )(FormInput.apply)(FormInput.unapply)
      )

    def failure(badForm: Form[FormInput]): Future[Result] = {
      Future.successful(BadRequest(badForm.errorsAsJson))
    }

    def success(input: FormInput): Future[Result] = {
      logger.trace(
        message = s"fetching unique values of column ${input.column}, SQL ${input.sqlId} ...")

      sparkValidCall(input.sqlId,
        Ok(Json.toJson(
          sparkService.unique(parquetPath(input.sqlId), input.column, input.n))))
    }

    form.bindFromRequest().fold(failure, success)
  }

  def allSpark(sqlId: Long): Action[AnyContent] = AuthAction.async { implicit request =>
    sparkValidCall(sqlId,
      Ok(Json.toJson(sparkService.all(parquetPath(sqlId)).map(Json.parse)))
    )
  }

  def topSpark(): Action[AnyContent] = AuthAction.async { implicit request =>
    case class FormInput(sqlId: Long, n: Int)

    val form: Form[FormInput] =
      Form(
        mapping(
          "sqlId" -> longNumber,
          "n" -> number
        )(FormInput.apply)(FormInput.unapply)
      )

    def failure(badForm: Form[FormInput]): Future[Result] = {
      Future.successful(BadRequest(badForm.errorsAsJson))
    }

    def success(input: FormInput): Future[Result] = {
      logger.trace(message = s"query spark on SQL ${input.sqlId} ...")

      sparkValidCall(input.sqlId,
        Ok(Json.toJson(
          sparkService.top(parquetPath(input.sqlId), input.n).map(Json.parse))))
    }

    form.bindFromRequest().fold(failure, success)
  }

  def columnsReport(sqlId: Long): Action[AnyContent] = AuthAction.async { implicit request =>
    sparkValidCall(sqlId,
      Ok(Json.toJson(sparkService.columns(parquetPath(sqlId)))))
  }

  def fullTextCount(table: String, query: String): Action[AnyContent] = AuthAction.async { implicit request =>
    repo.fullTextCount(table, query).map { count =>
      Ok(Json.toJson(UpdResource(count)))
    }
  }

  def fullTextSql(query: String, maxPerPage: Int, pageIdx: Int): Action[AnyContent] = AuthAction.async { implicit request =>
    repo.ftsPagedSql(query, maxPerPage, pageIdx).map { seq =>
      Ok(Json.toJson(seq))
    }
  }

}
