package models

import DB.session

object User {
    val session = DB.session
    val json_path = "hdfs://localhost:9000/user/Roman/hebrew/users.json"
    val dataframe = session.read.option("multiline", "true").json(json_path)
    dataframe.createOrReplaceTempView("users")

    def show_all() = {
        session.sql("SELECT * FROM users").show()
    }
}
class User(name: String, email: String){
}