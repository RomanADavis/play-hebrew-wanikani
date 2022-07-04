package models

import org.apache.spark.sql.SparkSession
import java.util.Properties

import org.apache.spark.sql.types.{StringType, StructField, StructType}
import org.apache.spark.sql.Row

// import spark.implicits._

object DB {
    System.setProperty("hadoop.home.dir", "~/hadoop/hadoop-3.3.3")

    val session = SparkSession
        .builder
        .appName("Hebrew-WaniKani")
        .config("spark.master", "local[*]")
        .enableHiveSupport()
        .getOrCreate()  

    val url = "jdbc:mysql://localhost:3306/hebrew"
    val user = "roman"
    val password = ""

}
