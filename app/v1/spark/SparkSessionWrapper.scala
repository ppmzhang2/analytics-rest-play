package v1.spark

import org.apache.spark.sql.SparkSession

trait SparkSessionWrapper {
  lazy val spark: SparkSession = SparkSession.builder
    .config("spark.local.dir", v1.SparkLocalDir)
    .master("local[*]")
    .appName("CRS Easy SQL")
    .getOrCreate
}
