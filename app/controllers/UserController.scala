package controllers

import javax.inject.Inject
import play.api.mvc._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits._

// Spark
import spark.SparkTest

import models.User
import models.DB.session

class UserController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  val session = models.DB.session
  val json_path = "hdfs://localhost:9000/user/Roman/hebrew/users.json"
  val dataframe = session.read.option("multiline", "true").json(json_path)
  dataframe.createOrReplaceTempView("users")

  def show_all() = Action { implicit request =>
    val dataframe = models.DB.session.sql(s"SELECT * FROM users ORDER BY id")
    Ok(views.html.users(dataframe))
  }

  def read(id: Int) = Action { implicit request =>
    val dataframe = models.DB.session.sql(s"SELECT * FROM users WHERE id = $id")
    val row = dataframe.first()
    Ok(views.html.user_read(row))
  }

  def post_signup() = Action {

  }

  def get_signup() = Action {
    Ok(views.html.signup())
  }

//   // A simple example to call Apache Spark
//   def test = Action { implicit request =>
//   	val sum = SparkTest.Example
//     Ok(views.html.test_args(s"A call to Spark, with result: $sum"))
//   }

//   // A non-blocking call to Apache Spark 
//   def testAsync = Action.async{
//   	val futureSum = Future{SparkTest.Example}
//     futureSum.map{ s => Ok(views.html.test_args(s"A non-blocking call to Spark with result: ${s + 1000}"))}
//   }

}
