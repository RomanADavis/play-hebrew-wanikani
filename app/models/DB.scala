package models

import org.apache.spark.sql.SparkSession
import java.util.Properties

import org.apache.spark.sql.types.{StringType, StructField, StructType}
import org.apache.spark.sql.Row
import org.apache.spark.SparkConf

// import spark.implicits._

object DB {

    val session = SparkSession
        .builder
        .appName("Hebrew-WaniKani")
        .config("spark.master", "local[*]")
        .config("spark.sql.crossJoin.enabled" , "true" )
        .getOrCreate() 

    val url = "jdbc:mysql://localhost:3306/hebrew"
    val user = "roman"
    val password = ""
}
