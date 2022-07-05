package controllers

import javax.inject.Inject

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits._

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.mvc.Results._

import models.User
import models.DB.session

class UserController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  def show_all() = Action { implicit request =>
    val dataframe = models.User.show_all()
    dataframe.show()
    Ok(views.html.users(dataframe))
  }

  def read(id: Int) = Action { implicit request =>
    val dataframe = models.User.read(id)
    dataframe.show()
    val row = dataframe.first()
    Ok(views.html.user_read(row))
  }

  def post_signup() = Action { request =>
    val postVals = request.body.asFormUrlEncoded
    postVals.map { args =>
      val username = args("username").head
      val password = args("password").head
      val role = args("role").head
      models.User.create(username, password, role)
      Redirect(routes.UserController.show_all())
    }.getOrElse(Ok("Oops"))
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
