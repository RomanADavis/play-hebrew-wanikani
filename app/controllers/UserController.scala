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
  // Formerly, the login page
  def home() = Action { implicit request =>
    models.User.show()
    Ok(views.html.users.home())
  }

  def all() = Action { implicit request =>
    val username: String = request.session.get("username").getOrElse("")

    if(username == ""){
        Redirect(routes.UserController.home()).flashing(
            "message" -> "Login to see users."
            )
    }
    val role: String = User.read(username).role
    val users = models.User.all()

    Ok(views.html.users.all(users))
  }
  
  // CREATE
  def signup() = Action { implicit request =>
    Ok(views.html.users.signup()).flashing("message" -> "Sign Ups Open")
  }

  // CREATE
  def create() = Action { request =>
    val postVals = request.body.asFormUrlEncoded
    postVals.map { args =>
      val username = args("username").head
      val password = args("password").head
      val role = args("role").head
      models.User.create(username, password, role)
      Redirect(routes.UserController.all()).flashing(
        "message" -> "Sign Up Successful")
    }.getOrElse(Ok("Oops"))
  }

  // READ
  def read(id: Int) = Action { implicit request =>
    val user: models.User = models.User.read(id)
    
    Ok(views.html.users.read(user))
  }

  // UPDATE
  def update(id: Int) = Action { implicit request =>
    val postVals = request.body.asFormUrlEncoded

    postVals.map { args =>
      val username = args("username").head
      val password = args("password").head
      val role = args("role").head
      models.User.update(id, username, password, role)
      Redirect(routes.UserController.read(id)).flashing(
        "message" -> "Update successful"
      )
    }.getOrElse(
      Redirect(routes.UserController.read(id)).flashing(
        "message" -> "Update failed")
    )
  }

  def delete(id: Int) = Action { implicit request =>
    val username: String = request.session.get("username").getOrElse("")
    if(username == ""){
        // I should actually return some sort of Bad Request or whatever.
        Redirect(routes.UserController.home()).flashing(
            "message" -> "Can't delete when not logged in."
        )
    }

    val role: String = request.session.get("role").getOrElse("")
    if(role != "admin"){
        Redirect(routes.UserController.home()).flashing(
            "message" -> "Can't delete users if not admin."
        )
    }

    models.User.delete(id)
    Redirect(routes.UserController.all()).flashing(
        "message" -> s"Sucessfully deleted user with id $id"
    )
    // }.getOrElse(Redirect(routes.UserController.show_all()).flashing(
    //     "message" -> s"Could not delete user with id $id")
    //     )
  }

  def login() = Action { implicit request =>
    val postVals = request.body.asFormUrlEncoded
    postVals.map { args =>
      // Get the username and passowrd
      val username = args("username").head
      val password = args("password").head
      // TODO: Figure out how to get roles
      //val role = args("role").head

      // Check if username is in the users table
      val user: User = models.User.read(username)

      // If not in users table, redirect back to login page with error message:
      // Incorrect username
      if(user.notfound()){
        Ok(views.html.users.home()).flashing(
            "message" -> "User not found")
      }

      // Check if password matches password for row in users table
      // If not, redirect back to login page with error message:
      // Incorrect password
      if(password != user.password){
        Ok(views.html.users.home()).flashing(
            "message" -> "Incorrect password")
      }

      // If password in matches, log the user in, somehow? ???
      Redirect(routes.UserController.all()).withSession(
        "username" -> username, "role" -> user.role
        ).flashing("message" -> "Login successful.")

    }.getOrElse(
        Redirect(routes.UserController.home()).flashing(
            "message" -> "User and password not found in header.")
    )
  }

  def logout() = Action { implicit request =>
    Redirect(routes.UserController.home()).withNewSession.flashing(
      "message" -> "Logged out")
  }

}
