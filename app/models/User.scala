package models

import org.apache.spark.sql.Row
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.types.{StringType, StructField, StructType, LongType}

import DB.session

class User(name: String, password: String, role: String){
    // Hmmm. This assums that we're only putting in one user at a time.
    // This may go awry.
    val id = session.sql(s"SELECT MAX(id) FROM users").first.getLong(0) + 1

    def dataframe(): org.apache.spark.sql.DataFrame = {
        return DB.session.createDataFrame(
            Seq((id, name, password, role)))
        .toDF("id", "name", "passowrd", "role")
    }
}

object User {
    val session = DB.session
    val json_path = "hdfs://localhost:9000/user/Roman/hebrew/users"
    // val json_path = "users.json"
    val schema = StructType(
        Array(
            StructField("id", LongType, nullable=false),
            StructField("name", StringType, nullable=false),
            StructField("password", StringType, nullable=false),
            StructField("role", StringType, nullable=false)
        )
    )

    var dataframe = session.read.schema(schema).json(json_path)
    dataframe.cache()
    dataframe.createOrReplaceTempView("users")

    def show_all(): org.apache.spark.sql.DataFrame = {
        // SELECT DISTINCT is a hack here: somehow, when I add a row to my table,
        // it gets added twice to the dataframe (but only once as a JSON);
        // should probably hunt down the bug later.
        return session.sql("SELECT DISTINCT * FROM users ORDER BY id")
    }

    def read(id: Int): org.apache.spark.sql.DataFrame = {
        return session.sql(s"SELECT * FROM users where id = $id")
    }

    def save(){
        dataframe.write.mode("overwrite").json(json_path)
        dataframe.cache()
    }

    def create(name: String, password: String, role: String): User = {
        show_all()
        val user = new User(name, password, role)

        user.dataframe().show()
        dataframe = dataframe.union(user.dataframe())
        dataframe.createOrReplaceTempView("users")
        
        save()
        return user
    }
}