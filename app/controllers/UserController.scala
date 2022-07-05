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
    val username: String = request.session.get("username").getOrElse("")
    val dataframe = models.User.all()
    dataframe.show()
    Ok(views.html.users(dataframe, username))
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

  def login_page(error: String = "") = Action {
    models.User.all().show()
    Ok(views.html.login_page(error))
  }

  def login() = Action { request =>
    val postVals = request.body.asFormUrlEncoded
    postVals.map { args =>
      // Get the username and passowrd
      val username = args("username").head
      val password = args("password").head
      // TODO: Figure out how to get roles
      //val role = args("role").head

      // Check if username is in the users table
      val user = models.User.read(username)
      val found : Long = user.count()

      // If not in users table, redirect back to login page with error message:
      // Incorrect username
      if(found == 0){
        Ok(views.html.login_page("User not found"))
      }

      // Check if password matches password for row in users table
      val user_password = user.first()getAs[String]("password")

      // If not, redirect back to login page with error message:
      // Incorrect password
      if(password != user_password){
        Ok(views.html.login_page("Incorrect password"))
      }

      // If password in matches, log the user in, somehow? ???
      Redirect(routes.UserController.show_all()).withSession("username" -> username)

    }.getOrElse(Ok("Oops"))
  }

}
