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

class RootController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  def view(letter: String) = Action { implicit request =>
    val username: String = request.session.get("username").getOrElse("")

    val parent : String = request.getQueryString("parent").getOrElse("")
    val column: String = request.getQueryString("column").getOrElse("root")
    val order: String = request.getQueryString("order").getOrElse("ASC")
    val roots = models.Root.view(letter, column, order, parent)

    Ok(views.html.roots.view(letter, roots))
  }

  // READ
  def read(root_name: String) = Action { implicit request =>
    val root: models.Root = models.Root.read(root_name)
    
    Ok(views.html.roots.read(root))
  }

  def root_counts(letter: String = "א") = Action { implicit request =>
    val root_counts = models.Root.child_root_counts(letter)
    root_counts.show()
    Ok(views.html.roots.root_counts(root_counts))
  }
}
