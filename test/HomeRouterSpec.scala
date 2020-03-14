import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test._
import v1.models._

import scala.concurrent.Future

class HomeRouterSpec extends PlaySpec with GuiceOneAppPerTest {
  private val token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9" +
    ".eyJuYmYiOjE1Nzc1NTY1MDAsImlhdCI6MTU3NzU1NjUwMCwidXNySWQiOjF9." +
    "NS_8HatPydcD1FY7nXngJYI9hGujxE7EYKAy74-2ozs"

  "User / SQL Lookup" should {

    "User lookup" in {
      val request = FakeRequest(GET, "/v1/home/user/by/3")
        .withHeaders("Token" -> token)
      val res: Future[Result] = route(app, request).get

      val user = Json.fromJson[User](contentAsJson(res)).get
      user.mustBe(User(3, "guest", "guest user", "guest user"))
    }

    "About me (demome)" in {
      val request = FakeRequest(GET, "/v1/home/user/me")
        .withHeaders("Token" -> token)
      val res: Future[Result] = route(app, request).get

      val user = Json.fromJson[User](contentAsJson(res)).get
      user.mustBe(User(1, "demome", "demo user", "demo user"))
    }

    "Sql lookup" in {
      val request = FakeRequest(GET, "/v1/home/sql/by/2")
        .withHeaders("Token" -> token)
      val res: Future[Result] = route(app, request).get

      Json.fromJson[Sql](contentAsJson(res)).get.content
        .mustBe("select * from table")
    }

    "All SQLs by user 2 (admin)" in {
      val request = FakeRequest(GET, "/v1/home/sqls/byusr/2")
        .withHeaders("Token" -> token)
      val res: JsValue = contentAsJson(route(app, request).get)

      Json.fromJson[SqlSumResource](res.head.get).get.desc.mustBe("sample sql test")
    }

  }

  "New SQL" should {
    "POST add new sql" in {
      val request = FakeRequest(POST, "/v1/home/sql/add")
        .withJsonBody(Json.parse(
          """{"desc":"new sql", "content": "the sql content"}"""))
        .withHeaders("Token" -> token)

      val res: JsValue = contentAsJson(route(app, request).get)

      Json.fromJson[InsResource](res).get.record.mustBe(5)
    }

    "fork a sql" in {
      val request = FakeRequest(GET, "/v1/home/sql/fork/by/2")
        .withHeaders("Token" -> token)

      val res: JsValue = contentAsJson(route(app, request).get)

      Json.fromJson[InsResource](res).get.record.mustBe(6)
    }
  }

  "Delete SQL" should {
    "fork a sql" in {
      val request = FakeRequest(GET, "/v1/home/sql/fork/by/2")
        .withHeaders("Token" -> token)

      val res: JsValue = contentAsJson(route(app, request).get)

      Json.fromJson[InsResource](res).get.record.mustBe(7)
    }

    "add a comment" in {
      val request = FakeRequest(POST, "/v1/home/comment/add")
        .withJsonBody(Json.parse(
          """{"sqlId":"7", "content":"now you see it", "reId":"0"}"""))
        .withHeaders("Token" -> token)

      val res: JsValue = contentAsJson(route(app, request).get)

      Json.fromJson[InsResource](res).get.record.mustBe(4)
    }

    "check comments" in {
      val request = FakeRequest(GET, "/v1/home/comment/bysql/7")
        .withHeaders("Token" -> token)
      val res: JsValue = contentAsJson(route(app, request).get)
      Json.fromJson[Comment](res.head.get).get.content.mustBe("now you see it")
    }

    "delete that SQL once" in {
      val request = FakeRequest(POST, "/v1/home/sql/del")
        .withJsonBody(Json.parse(
          """{"id":7}"""))
        .withHeaders("Token" -> token)

      val res: JsValue = contentAsJson(route(app, request).get)

      Json.fromJson[UpdResource](res).get.records.mustBe(1)
    }

    "delete that SQL again" in {
      val request = FakeRequest(POST, "/v1/home/sql/del")
        .withJsonBody(Json.parse(
          """{"id":7}"""))
        .withHeaders("Token" -> token)

      val res: JsValue = contentAsJson(route(app, request).get)

      Json.fromJson[MessageResource](res).get.explain.mustBe("SQL 7 not found")
    }

    "check comments again" in {
      val request = FakeRequest(GET, "/v1/home/comment/bysql/7")
        .withHeaders("Token" -> token)
      val res = contentAsString(route(app, request).get)
      res.mustBe("[]")
    }
  }

  "Star SQL" should {
    "like a post" in {
      val request = FakeRequest(POST, "/v1/home/sql/star")
        .withJsonBody(Json.parse(
          """{"sqlId":"2"}"""))
        .withHeaders("Token" -> token)

      val res: JsValue = contentAsJson(route(app, request).get)

      Json.fromJson[SqlStarResource](res).get.stars.mustBe(3)
    }

    "undo like a post" in {
      val request = FakeRequest(POST, "/v1/home/sql/star")
        .withJsonBody(Json.parse(
          """{"sqlId":"2"}"""))
        .withHeaders("Token" -> token)

      val res: JsValue = contentAsJson(route(app, request).get)

      Json.fromJson[SqlStarResource](res).get.stars.mustBe(2)
    }
  }

  "Comments" should {
    "add a comment" in {
      val request = FakeRequest(POST, "/v1/home/comment/add")
        .withJsonBody(Json.parse(
          """{"sqlId":"2", "content":"again?", "reId":"0"}"""))
        .withHeaders("Token" -> token)

      val res: JsValue = contentAsJson(route(app, request).get)

      Json.fromJson[InsResource](res).get.record.mustBe(5)
    }

    "add a reply" in {
      val request = FakeRequest(POST, "/v1/home/comment/add")
        .withJsonBody(Json.parse(
          """{"sqlId":"2", "content":"you bet", "reId":"4"}"""))
        .withHeaders("Token" -> token)

      val res: JsValue = contentAsJson(route(app, request).get)

      Json.fromJson[InsResource](res).get.record.mustBe(6)
    }
  }

  "Report" should {
    "add a report option" in {
      val request = FakeRequest(POST, "/v1/home/report/add")
        .withJsonBody(Json.parse(
          """{"sqlId":"2", "dataOption":"{}", "chartOption":"{}"}"""))
        .withHeaders("Token" -> token)

      val res: JsValue = contentAsJson(route(app, request).get)

      Json.fromJson[InsResource](res).get.record.mustBe(3)
    }

    "fork a report option" in {
      val request = FakeRequest(POST, "/v1/home/report/fork")
        .withJsonBody(Json.parse(
          """{"rptId":"1", "sqlId":"2"}"""))
        .withHeaders("Token" -> token)

      val res: JsValue = contentAsJson(route(app, request).get)

      Json.fromJson[InsResource](res).get.record.mustBe(4)
    }

    "lookup one report option" in {
      val request = FakeRequest(GET, "/v1/home/report/option/by/1")
        .withHeaders("Token" -> token)

      val res: JsValue = contentAsJson(route(app, request).get)

      val data = Json.fromJson[DataOption](
        Json.fromJson[ReportResource](res.result.get).get.data).get.groupExprs

      data.mustBe(Seq(GroupExpr(Vector("country", "gender"), "Any Group Name")))
    }

    "lookup report options" in {
      val request = FakeRequest(GET, "/v1/home/reports/option/by/3")
        .withHeaders("Token" -> token)

      val res: JsValue = contentAsJson(route(app, request).get)

      val data = Json.fromJson[DataOption](
        Json.fromJson[ReportResource](res.head.get).get.data).get.groupExprs

      data.mustBe(Seq(GroupExpr(Vector("country", "gender"), "Any Group Name")))
    }

    "edit report options" in {
      val request = FakeRequest(POST, "/v1/home/report/edit")
        .withJsonBody(Json.parse(
          """{"rptId":"3", "dataOption":"{}", "chartOption":"{}"}"""))
        .withHeaders("Token" -> token)

      val res: JsValue = contentAsJson(route(app, request).get)

      Json.fromJson[UpdResource](res).get.records.mustBe(1)
    }

    "delete report 3" in {
      val request = FakeRequest(POST, "/v1/home/report/del")
        .withJsonBody(Json.parse(
          """{"id":"3"}"""))
        .withHeaders("Token" -> token)

      val res: JsValue = contentAsJson(route(app, request).get)

      Json.fromJson[UpdResource](res).get.records.mustBe(1)
    }

    "delete report 4" in {
      val request = FakeRequest(POST, "/v1/home/report/del")
        .withJsonBody(Json.parse(
          """{"id":"4"}"""))
        .withHeaders("Token" -> token)

      val res: JsValue = contentAsJson(route(app, request).get)

      Json.fromJson[UpdResource](res).get.records.mustBe(1)
    }
  }

  "Full Text Search" should {
    "full text count" in {
      val request = FakeRequest(GET, "/v1/home/ft/count?table=sql&query=select")
        .withHeaders("Token" -> token)

      val res: JsValue = contentAsJson(route(app, request).get)

      Json.fromJson[UpdResource](res).get.records.mustBe(5)
    }

    "full text sql search" in {
      val request = FakeRequest(GET, "/v1/home/ft/sql?query=sample%20sql&max=10&page=1")
        .withHeaders("Token" -> token)

      val res: JsValue = contentAsJson(route(app, request).get)

      Json.fromJson[SqlSumResource](res.head.get).get.desc.mustBe("sample sql test")
    }
  }

}
