package v1.spark

import java.util.Properties

import javax.inject.Singleton
import org.apache.spark.sql.types.StringType
import org.apache.spark.sql.{Column, functions}
import v1.models._

@Singleton
class SparkService extends SparkSessionWrapper {

  import spark.implicits._

  private val teraUrl = v1.TeraUrl
  private val connProps = {
    val connProperties = new Properties()
    connProperties.put("user", v1.TeraUser)
    connProperties.put("password", v1.TeraPassword)
    connProperties.setProperty("Driver", v1.TeraDriverClass)
    connProperties
  }


  def count(teraTable: String): Int = {
    val pushDownQry =
      s"""
         |(SELECT sum(1) AS row_count
         |   FROM $teraTable) cnt
         |""".stripMargin
    spark.read.jdbc(teraUrl, pushDownQry, connProps).select("row_count")
      .first.getInt(0)
  }

  def teraToParquet(tableName: String, path: String): String = {
    val table = spark.read.jdbc(teraUrl, tableName, connProps)
    table.write.option("compression", "snappy").parquet(path)
    path
  }

  def all(path: String): Array[String] = {
    spark.read.parquet(path).toJSON.collect()
  }

  def top(path: String, n: Int): Array[String] = {
    spark.read.parquet(path).limit(n).toJSON.collect()
  }

  def columns(path: String): Array[String] = {
    spark.read.parquet(path).columns
  }

  def unique(path: String, column: String, n: Int): Array[String] = {
    spark.read.parquet(path).select(column)
      .withColumn(column, functions.col(column).cast(StringType))
      .distinct().map(_.getAs[String](column)).take(n)
  }

  def grouped(path: String,
              filters: Seq[FilterExpr],
              groupExprs: Seq[GroupExpr],
              pivotExpr: PivotExpr,
              aggExprs: Seq[AggExpr],
              selExprs: Seq[SelExpr]): Array[String] = {
    val df = {
      // combine group fields
      case class GroupArgs(column: Column, alias: String)
      val groups = groupExprs.map { ge =>
        (ge.fields.map(functions.col)
          .reduce(functions.concat(_, functions.lit(literal = " - "), _)), ge.alias)
      }.map(tp => GroupArgs.apply _ tupled tp)
      // rename with group alias
      val dfAll = groups.foldLeft(spark.read.parquet(path)) { (z, group) =>
        z.withColumn(group.alias, group.column)
      }
      // filtered
      val dfFiltered = {
        if (filters.isEmpty) dfAll else {
          filters.map { fe =>
            fe.field + fe.operator + fe.operand
          }.foldLeft(dfAll)((df, str) => df.filter(str))
        }
      }
      val dfGrouped = dfFiltered.groupBy(
        groups.map(ga => functions.col(ga.alias)): _*)
      if (pivotExpr.values.isEmpty) {
        dfGrouped
      } else {
        dfGrouped.pivot(pivotExpr.column, pivotExpr.values)
      }
    }
    df.agg(
      (aggExprs.head.field, aggExprs.head.func),
      aggExprs.drop(1).map(ae => (ae.field, ae.func)): _*)
      .orderBy(groupExprs.map(ge => functions.asc(ge.alias)): _*)
      .selectExpr(groupExprs.map("`" + _.alias + "`") ++
        selExprs.map { tp =>
          tp.expr.map(ae => ae.field + ae.operator).reduce(_ + _) + " AS " + tp.alias
        }: _*)
      .na.fill(value = 0, selExprs.map(_.alias.replace("`", "")).toArray)
      .toJSON.collect()
  }

}
