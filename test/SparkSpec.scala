import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.libs.json.{JsValue, Json}
import v1.models.{AggExpr, ArithExpr, FilterExpr, GroupExpr, PivotExpr, SelExpr}
import v1.spark.SparkService

class SparkSpec extends PlaySpec with GuiceOneAppPerTest {
  private val sparkService = new SparkService

  "PostRouter" should {

    "spark all" in {
      val path = "data/region.parquet"
      val json = sparkService.all(path)

      val res = Json.toJson(json.map(Json.parse))

      val exp: JsValue = Json.obj(
        "R_REGIONKEY" -> 0,
        "R_NAME" -> "AFRICA",
        "R_COMMENT" -> "lar deposits. blithe"
      )

      res.head.mustBe(exp.result)
    }

    "report columns" in {
      val path = "data/userdata1.parquet"
      val columns = sparkService.columns(path)
      val exp: Seq[String] = Seq("registration_dttm",
        "id","first_name","last_name","email","gender",
        "ip_address","cc","country","birthdate","salary","title","comments")
      columns.mustBe(exp)
    }

    "grouped with valid filters" in {
      val path = "data/userdata1.parquet"
      val filters = Seq(FilterExpr("country", "==", "'Afghanistan'"),
        FilterExpr("gender", "==", "'Female'"))
      val grpExprs = Seq(GroupExpr(Seq("country", "gender"), "An Odd Alias"))
      val pivotExpr = PivotExpr("gender", Seq())
      val aggExprs = Seq(AggExpr("salary", "sum"), AggExpr("salary", "count"))
      val selExprs = Seq(
        SelExpr(Seq(ArithExpr("`sum(salary)`", "+"), ArithExpr("1", "")),
          "`Total Salary`"),
        SelExpr(Seq(ArithExpr("`sum(salary)`", "/"), ArithExpr("`count(salary)`", "")),
          "`Average`"))
      val json = sparkService.grouped(path, filters, grpExprs, pivotExpr, aggExprs, selExprs)

      val res = Json.toJson(json.map(Json.parse))

      val exp: JsValue = Json.obj(
        "An Odd Alias" -> "Afghanistan - Female",
        "Total Salary" -> 220303.18,
        "Average" -> 220302.18
      )

      res.head.mustBe(exp.result)
    }

    "grouped with no filter" in {
      val path = "data/userdata1.parquet"
      val filters = Seq()
      val grpExprs = Seq(GroupExpr(Seq("country", "gender"), "An Odd Alias"))
      val pivotExpr = PivotExpr("gender", Seq())
      val aggExprs = Seq(AggExpr("salary", "sum"), AggExpr("salary", "count"))
      val selExprs = Seq(
        SelExpr(Seq(ArithExpr("`sum(salary)`", "+"), ArithExpr("1", "")),
          "`Total Salary`"),
        SelExpr(Seq(ArithExpr("`sum(salary)`", "/"), ArithExpr("`count(salary)`", "")),
          "`Average`"))
      val json = sparkService.grouped(path, filters, grpExprs, pivotExpr, aggExprs, selExprs)

      val res = Json.toJson(json.map(Json.parse))

      val exp: JsValue = Json.obj(
        "An Odd Alias" -> "\"Bonaire - Male",
        "Total Salary" -> 0,
        "Average" -> 0
      )

      res.head.mustBe(exp.result)
    }

    "grouped with pivot option" in {
      val path = "data/userdata1.parquet"
      val filters = Seq(FilterExpr("country", "==", "'China'"))
      val grpExprs = Seq(GroupExpr(Seq("country"), "An Odd Alias"))
      val pivotExpr = PivotExpr("gender", Seq("Male", "Female"))
      val aggExprs = Seq(AggExpr("salary", "sum"), AggExpr("salary", "count"))
      val selExprs = Seq(
        SelExpr(Seq(ArithExpr("`Male_sum(salary)`", "")), "`Male Salary`"),
        SelExpr(Seq(ArithExpr("`Female_sum(salary)`", "")), "`Female Salary`"),
        SelExpr(Seq(ArithExpr("`Female_sum(salary)`", "/"), ArithExpr("`Female_count(salary)`", "")),
          "`Female Average`"))
      val json = sparkService.grouped(path, filters, grpExprs, pivotExpr, aggExprs, selExprs)

      val res = Json.toJson(json.map(Json.parse))

      val exp: JsValue = Json.obj(
        "An Odd Alias" -> "China",
        "Male Salary" -> 14395011.689999998,
        "Female Salary" -> 13458636.790000003,
        "Female Average" -> 151220.63808988768
      )

      res.head.mustBe(exp.result)
    }

    "grouped with multiple group options" in {
      val path = "data/userdata1.parquet"
      val filters = Seq(FilterExpr("country", "==", "'Canada'"),
        FilterExpr("last_name", "==", "'Freeman'"))
      val grpExprs = Seq(GroupExpr(Seq("country"), "Country"),
        GroupExpr(Seq("last_name"), "Last Name"))
      val pivotExpr = PivotExpr("gender", Seq("Male", "Female"))
      val aggExprs = Seq(AggExpr("salary", "sum"), AggExpr("salary", "count"))
      val selExprs = Seq(
        SelExpr(Seq(ArithExpr("`Male_sum(salary)`", "")), "`Male Salary`"),
        SelExpr(Seq(ArithExpr("`Female_sum(salary)`", "")), "`Female Salary`"),
        SelExpr(Seq(ArithExpr("`Male_sum(salary)`", "/"), ArithExpr("`Male_count(salary)`", "")),
          "`Male Average`"))
      val json = sparkService.grouped(path, filters, grpExprs, pivotExpr, aggExprs, selExprs)

      val res = Json.toJson(json.map(Json.parse))

      val exp: JsValue = Json.obj(
        "Country" -> "Canada",
        "Last Name" -> "Freeman",
        "Male Salary" -> 316587.93000000005,
        "Female Salary" -> 0,
        "Male Average" -> 158293.96500000003
      )

      res.head.mustBe(exp.result)
    }
  }

}
