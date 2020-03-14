package v1

import javax.inject.Inject
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._

/**
  * Routes and URLs to the PostResource controller.
  */
class HomeRouter @Inject()(controller: HomeController) extends SimpleRouter {
  val prefix = "/v1/home"

  override def routes: Routes = {
    case GET(p"/user/by/$id") =>
      controller.lookupUser(id.toLong)

    case GET(p"/user/me") =>
      controller.aboutMe()

    case GET(p"/sqls/all") =>
      controller.allSql()

    case GET(p"/sqls/byusr/$usrId") =>
      controller.allSqlBy(usrId.toLong)

    case GET(p"/sql/by/$id") =>
      controller.lookupSql(id.toLong)

    case POST(p"/sql/edit") =>
      controller.editSql()

    case POST(p"/sql/star") =>
      controller.updateStar()

    case POST(p"/sql/add") =>
      controller.addSql()

    case POST(p"/sql/del") =>
      controller.delSql()

    case GET(p"/sql/fork/by/$sqlId") =>
      controller.forkSql(sqlId.toLong)

    case GET(p"/comment/bysql/$sqlId") =>
      controller.allCommentsBySql(sqlId.toLong)

    case POST(p"/comment/add") =>
      controller.addComment()

    case GET(p"/report/option/by/$id") =>
      controller.lookupReportResource(id.toLong)

    case POST(p"/report/add") =>
      controller.addReport()

    case POST(p"/report/fork") =>
      controller.forkReport()

    case POST(p"/report/edit") =>
      controller.editReport()

    case POST(p"/report/del") =>
      controller.delReport()

    case GET(p"/reports/option/by/$sqlId") =>
      controller.allReportResourceBySql(sqlId.toLong)

    case GET(p"/reports/by/$sqlId") =>
      controller.allVisResourceBySql(sqlId.toLong)

    case GET(p"/reports/columns/by/$sqlId") =>
      controller.columnsReport(sqlId.toLong)

    case POST(p"/tera/exec") =>
      controller.tdExec()

    case POST(p"/tera/fx") =>
      controller.tdToSpark()

    case GET(p"/spark/by/$sqlId") =>
      controller.allSpark(sqlId.toLong)

    case POST(p"/spark/top") =>
      controller.topSpark()

    case GET(p"/spark/valid/$sqlId") =>
      controller.sparkValidate(sqlId.toLong)

    case POST(p"/spark/column/unique") =>
      controller.sparkUnique()

    case GET(p"/ft/count" ? q"table=$table" ? q"query=$query") =>
      controller.fullTextCount(table, query)

    case GET(p"/ft/sql" ? q"query=$query" ? q"max=$maxPerPage" ? q"page=$pageIdx") =>
      controller.fullTextSql(query, maxPerPage.toInt, pageIdx.toInt)
  }

}
