// package controllers

// import javax.inject.Inject

// import scala.concurrent.Future
// import scala.concurrent.ExecutionContext.Implicits._

// import play.api.mvc._
// import play.api.data._
// import play.api.data.Forms._
// import play.api.mvc.Results._

// import models.User
// import models.DB.session

// class WordController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
//   def view(letter: String) = Action { implicit request =>
//     val username: String = request.session.get("username").getOrElse("")

//     val parent : String = request.getQueryString("parent").getOrElse("")
//     val column: String = request.getQueryString("column").getOrElse("root")
//     val order: String = request.getQueryString("order").getOrElse("ASC")
//     val words = models.Word.view(letter, column, order, parent)

//     val letters = models.Letter.all()
    
//     Ok(views.html.words.view(letter, words, letters))
//   }

//   // READ
//   def read(word_name: String) = Action { implicit request =>
//     val root: models.Word = models.Word.read(word_name)
    
//     Ok(views.html.words.read(root))
//   }
// }
