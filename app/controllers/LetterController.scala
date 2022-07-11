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

class LetterController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  def all() = Action { implicit request =>
    val username: String = request.session.get("username").getOrElse("")

    if(username == ""){
        Redirect(routes.UserController.home()).flashing(
            "message" -> "Login to see users."
            )
    }

    val column: String = request.getQueryString("column").getOrElse("letter")
    val order: String = request.getQueryString("order").getOrElse("ASC")
    val letters = models.Letter.all(column, order)

    Ok(views.html.letters.all(letters))
  }

  // READ
  def read(name: String) = Action { implicit request =>
    val letter: models.Letter = models.Letter.read(name)
    
    Ok(views.html.letters.read(letter))
  }
}
