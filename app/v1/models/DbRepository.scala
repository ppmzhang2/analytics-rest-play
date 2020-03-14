package v1.models

import java.time.LocalDate

import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.Json
import slick.jdbc.{GetResult, JdbcProfile}
import slick.lifted.{ForeignKeyQuery, ProvenShape}

import scala.concurrent.{ExecutionContext, Future}

/**
  * A repository for post.
  *
  * @param dbConfigProvider The Play db config provider. Play will inject this for you.
  */
@Singleton
class DbRepository @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  // We want the JdbcProfile for this provider
  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  // These imports are important, the first one brings db into scope, which will let you do the actual db operations.
  // The second one brings the Slick DSL into scope, which lets you define the table and other queries.

  import dbConfig._
  import profile.api._

  def addUser(ntid: String, name: String, title: String): Future[Long] = db.run {
    (userQuery.map(r => (r.ntid, r.name, r.title))
      returning userQuery.map(_.id)) += (ntid, name, title)
  }

  def addSql(usrid: Long, desc: String, content: String): Future[Long] = db.run {
    // We create a projection of just the name and age columns, since we're not inserting a value for the id column
    (sqlQuery.map(p =>
      (p.usrid, p.desc, p.content, p.stars, p.execution, p.exportation, p.updated))
      // Now define it to return the id, because we want to know what id was generated for the person
      returning sqlQuery.map(_.id)
      // And finally, insert the person into the database
      ) += (usrid, desc, content, 0, 0, 0, LocalDate.now())
  }

  def editSql(sqlId: Long, desc: String, content: String): Future[Int] = db.run {
    sqlQuery.filter(_.id === sqlId).map(r => (r.desc, r.content, r.updated))
      .update(desc, content, LocalDate.now())
  }

  def delSql(sqlId: Long): Future[Int] = db.run {
    sqlQuery.filter(_.id === sqlId).delete
  }

  def updateSqlExecStatus(sqlId: Long, execStatus: Int): Future[Int] = db.run {
    sqlQuery.filter(_.id === sqlId).map(_.execution).update(execStatus)
  }

  def updateSqlExpStatus(sqlId: Long, expStatus: Int): Future[Int] = db.run {
    sqlQuery.filter(_.id === sqlId).map(_.exportation).update(expStatus)
  }

  def lookupUser(usrid: Long): Future[Option[User]] = db.run {
    userQuery.filter(_.id === usrid).result.headOption
  }

  def lookupUserNt(ntid: String): Future[Option[User]] = db.run {
    userQuery.filter(_.ntid === ntid).result.headOption
  }

  def lookupSql(sqlid: Long): Future[Option[Sql]] = db.run {
    sqlQuery.filter(_.id === sqlid).result.headOption
  }

  def lookupComment(commentId: Long): Future[Option[Comment]] = db.run {
    commentQuery.filter(_.id === commentId).result.headOption
  }

  def allSql(): Future[Seq[SqlSumResource]] = db.run {
    sqlQuery.flatMap { sql =>
      userQuery.filter(_.id === sql.usrid).map { user =>
        (sql.id, user.id, user.ntid, sql.desc, sql.stars, sql.updated)
      }
    }.result
  }.map { seq =>
    seq.map { tp =>
      SqlSumResource(tp._1, tp._2, tp._3, tp._4, tp._5, tp._6)
    }
  }

  def allSqlBy(usrid: Long): Future[Seq[SqlSumResource]] = db.run {
    userQuery.filter(_.id === usrid).flatMap { user =>
      sqlQuery.filter(_.usrid === user.id).map { sql =>
        (sql.id, user.id, user.ntid, sql.desc, sql.stars, sql.updated)
      }
    }.result
  }.map { seq =>
    seq.map { tp =>
      SqlSumResource(tp._1, tp._2, tp._3, tp._4, tp._5, tp._6)
    }
  }

  def allCommentsBySql(sqlid: Long): Future[Seq[Comment]] = db.run {
    commentQuery.filter(_.sqlid === sqlid).sorted(_.created).result
  }

  def forkSql(sqlid: Long, usrid: Long): Future[Option[Long]] = {
    lookupSql(sqlid).flatMap {
      case Some(sql) => addSql(usrid, sql.desc, sql.content).map(Some(_))
      case _ => Future.successful(None)
    }
  }

  def updateStar(sqlid: Long, usrid: Long): Future[Option[Int]] = {
    def sqlLiked(sqlid: Long, usrid: Long): Future[Boolean] = db.run {
      starQuery.filter(r => r.sqlid === sqlid && r.usrid === usrid)
        .result.headOption.map {
        case Some(_) => true
        case _ => false
      }
    }

    def currentStar(sqlid: Long): Future[Option[Int]] = db.run {
      sqlQuery.filter(_.id === sqlid).map(_.stars).result.headOption
    }

    def addStar(sqlid: Long, inc: Int): Future[Int] = {
      currentStar(sqlid).flatMap { op =>
        db.run {
          sqlQuery.filter(_.id === sqlid).map(_.stars).update(op.getOrElse(0) + inc)
        }
      }
    }

    sqlLiked(sqlid, usrid).flatMap { liked =>
      if (liked) {
        addStar(sqlid, -1).flatMap { _ =>
          db.run {
            starQuery.filter(r => r.sqlid === sqlid && r.usrid === usrid).delete
          }
        }
      } else {
        addStar(sqlid, 1).flatMap { _ =>
          db.run {
            starQuery.map(r => (r.sqlid, r.usrid)) += (sqlid, usrid)
          }
        }
      }
    }.flatMap { _ =>
      db.run {
        sqlQuery.filter(r => r.id === sqlid).map(r => r.stars).result.headOption
      }
    }
  }

  def addCommont(usrid: Long, sqlid: Long, content: String, reid: Option[Long]): Future[Long] = {
    def helper(usrid: Long, sqlid: Long, content: String, reid: Long, reusrid: Long): Future[Long] = db.run {
      (commentQuery.map(r => (r.reid, r.usrid, r.reusrid, r.sqlid, r.content, r.created))
        returning commentQuery.map(_.id)
        ) += (reid, usrid, reusrid, sqlid, content, LocalDate.now())
    }

    lookupComment(reid.getOrElse(0L))
      .flatMap {
        case Some(comment) => helper(usrid, sqlid, content, comment.reid, comment.reusrid)
        case _ => helper(usrid, sqlid, content, 0, 0)
      }
  }

  def lookupReport(id: Long): Future[Option[Report]] = db.run {
    reportQuery.filter(_.id === id).result.headOption
  }

  def lookupReportResource(id: Long): Future[Option[ReportResource]] = {
    lookupReport(id).flatMap {
      case Some(rpt) => lookupSql(rpt.sqlid).map {
        case Some(sql) => Some {
          ReportResource(rpt.id, sql.id, sql.usrid,
            Json.toJson(Json.parse(rpt.data)),
            Json.toJson(Json.parse(rpt.chart)))
        }
        case _ => None
      }
      case _ => Future.successful(None)
    }
  }

  def addReport(sqlId: Long, dataOption: String, chartOption: String): Future[Long] = db.run {
    (reportQuery.map(r => (r.sqlid, r.data, r.chart))
      returning reportQuery.map(_.id)
      ) += (sqlId, dataOption, chartOption)
  }

  def forkReport(rptId: Long, sqlId: Long): Future[Option[Long]] = {
    lookupReport(rptId).flatMap {
      case Some(rpt) => addReport(rpt.sqlid, rpt.data, rpt.chart).map(Some(_))
      case _ => Future.successful(None)
    }
  }

  def editReport(rptId: Long, dataOption: String, chartOption: String): Future[Int] = db.run {
    reportQuery.filter(_.id === rptId).map(r => (r.data, r.chart))
      .update(dataOption, chartOption)
  }

  def allReportResourceBySql(sqlId: Long): Future[Seq[ReportResource]] = db.run {
    reportQuery.filter(_.sqlid === sqlId).flatMap { rpt =>
      sqlQuery.filter(_.id === sqlId).map { sql =>
        (rpt.id, sql.id, sql.usrid, rpt.data, rpt.chart)
      }
    }.result
  }.map { seq =>
    seq.map { tp =>
      ReportResource(tp._1, tp._2, tp._3,
        Json.toJson(Json.parse(tp._4)),
        Json.toJson(Json.parse(tp._5)))
    }
  }

  def delReport(id: Long): Future[Int] = db.run {
    reportQuery.filter(_.id === id).delete
  }

  def fullTextCount(table: String, query: String): Future[Int] = db.run {
    sql"""SELECT sum(1) AS count
            FROM ft_search_data($query, 0, 0) AS t
           WHERE t."TABLE" = $table
       """.as[Int].head
  }

  def fullTextData(pattern: String, table: String): Future[Seq[FullTextData]] = db.run {
    implicit val getResult: AnyRef with GetResult[FullTextData] = GetResult { r =>
      FullTextData(r.nextLong, r.nextDouble)
    }

    sql"""SELECT t."KEYS"[1] AS "id"
               , t."SCORE"   AS "score"
            FROM ft_search_data($pattern, 0, 0) AS t
           WHERE t."TABLE" = $table
           ORDER BY t."SCORE" DESC;
       """.as[FullTextData]
  }

  def ftsPagedSql(query: String, maxPerPage: Int, pageIdx: Int): Future[Seq[SqlSumResource]] = {
    fullTextData(query, table = "sql").map { seq =>
      seq.slice(maxPerPage * (pageIdx - 1), maxPerPage * pageIdx)
    }.flatMap { seq =>
      db.run {
        sqlQuery.filter(_.id.inSet(seq.map(_.id))).flatMap { sql =>
          userQuery.filter(_.id === sql.usrid).map { user =>
            (sql.id, sql.usrid, user.ntid, sql.desc, sql.stars, sql.updated)
          }
        }.result
      }.map { seq =>
        seq.map { tp =>
          SqlSumResource.apply _ tupled tp
        }
      }
    }
  }

  /**
    * Here we define the tables
    */
  private class UserTable(tag: Tag) extends Table[User](tag, "user") {
    def id: Rep[Long] =
      column[Long]("id", O.PrimaryKey, O.AutoInc)

    def ntid: Rep[String] =
      column[String]("ntid")

    def name: Rep[String] =
      column[String]("name")

    def title: Rep[String] =
      column[String]("title")

    def * : ProvenShape[User] =
      (id, ntid, name, title) <> ((User.apply _).tupled, User.unapply)
  }

  private class SqlTable(tag: Tag) extends Table[Sql](tag, "sql") {
    def id: Rep[Long] =
      column[Long]("id", O.PrimaryKey, O.AutoInc)

    def usrid: Rep[Long] =
      column[Long]("usrid")

    def desc: Rep[String] =
      column[String]("desc")

    def content: Rep[String] =
      column[String]("content")

    def stars: Rep[Int] =
      column[Int]("stars")

    def execution: Rep[Int] =
      column[Int]("execution")

    def exportation: Rep[Int] =
      column[Int]("exportation")

    def updated: Rep[LocalDate] =
      column[LocalDate]("updated")

    def * : ProvenShape[Sql] =
      (id, usrid, desc, content, stars, execution, exportation, updated) <>
        ((Sql.apply _).tupled, Sql.unapply)

    def usridFk: ForeignKeyQuery[UserTable, User] =
      foreignKey(name = "NTIDFK", usrid, userQuery)(_.id, onDelete = ForeignKeyAction.Cascade)
  }

  private class StarTable(tag: Tag) extends Table[Star](tag, "star") {
    def sqlid: Rep[Long] =
      column[Long]("sqlid")

    def usrid: Rep[Long] =
      column[Long]("usrid")

    def * : ProvenShape[Star] =
      (sqlid, usrid) <> ((Star.apply _).tupled, Star.unapply)

    def sqlidFk: ForeignKeyQuery[SqlTable, Sql] =
      foreignKey(name = "SQLIDFK", sqlid, sqlQuery)(_.id, onDelete = ForeignKeyAction.Cascade)

    def usridFk: ForeignKeyQuery[UserTable, User] =
      foreignKey(name = "NTIDFK", usrid, userQuery)(_.id, onDelete = ForeignKeyAction.Cascade)

    def pkid = primaryKey("pkid", (sqlid, usrid))
  }

  private class CommentTable(tag: Tag) extends Table[Comment](tag, "comment") {
    def id: Rep[Long] =
      column[Long]("id", O.PrimaryKey, O.AutoInc)

    def reid: Rep[Long] =
      column[Long]("reid")

    def usrid: Rep[Long] =
      column[Long]("usrid")

    def reusrid: Rep[Long] =
      column[Long]("reusrid")

    def sqlid: Rep[Long] =
      column[Long]("sqlid")

    def content: Rep[String] =
      column[String]("content")

    def created: Rep[LocalDate] =
      column[LocalDate]("created")

    def * : ProvenShape[Comment] =
      (id, reid, usrid, reusrid, sqlid, content, created) <> ((Comment.apply _).tupled, Comment.unapply)

    def sqlidFk: ForeignKeyQuery[SqlTable, Sql] =
      foreignKey(name = "SQLIDFK", sqlid, sqlQuery)(_.id, onDelete = ForeignKeyAction.Cascade)

  }

  private class ReportTable(tag: Tag) extends Table[Report](tag, "report") {
    def id: Rep[Long] =
      column[Long]("id", O.PrimaryKey, O.AutoInc)

    def sqlid: Rep[Long] =
      column[Long]("sqlid")

    def data: Rep[String] =
      column[String]("data")

    def chart: Rep[String] =
      column[String]("chart")

    def * : ProvenShape[Report] =
      (id, sqlid, data, chart) <> ((Report.apply _).tupled, Report.unapply)

    def sqlidFk: ForeignKeyQuery[SqlTable, Sql] =
      foreignKey(name = "SQLIDFK", sqlid, sqlQuery)(_.id, onDelete = ForeignKeyAction.Cascade)
  }

  /**
    * The starting point for all queries on the post table.
    */
  private lazy val userQuery = TableQuery[UserTable]

  private lazy val sqlQuery = TableQuery[SqlTable]

  private lazy val starQuery = TableQuery[StarTable]

  private lazy val commentQuery = TableQuery[CommentTable]

  private lazy val reportQuery = TableQuery[ReportTable]
}
