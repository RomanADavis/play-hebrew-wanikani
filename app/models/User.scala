package models

import org.apache.spark.sql.Row
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.types.{StringType, StructField, StructType, LongType}

import DB.session

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

    // Hack so that methods return User even when not found.
    val empty: User = new User("NULL", "NULL", "NULL", -1)

    def show() = {
        // SELECT DISTINCT is a hack here: somehow, when I add a row to my table,
        // it gets added twice to the dataframe (but only once as a JSON);
        // should probably hunt down the bug later.
        dataframe = session.sql("SELECT DISTINCT * FROM users ORDER BY id")
        dataframe.show()
    }

    def all(): Array[User] = {
        show()
        
        return dataframe.rdd.map(row =>
            new User(row)
        ).collect 
    }

    def save() = {
        dataframe.createOrReplaceTempView("users")
        dataframe.write.mode("overwrite").json(json_path)
        dataframe.cache()
    }

    def create(name: String, password: String, role: String, id: Long = -1L): User = {
        all()
        val user = new User(name, password, role, id)

        user.dataframe().show()
        dataframe = dataframe.union(user.dataframe())
        
        save()
        return user
    }

    def read(id: Int): User = {
        // TODO: Get this to return a user (and update the corresponding views)
        val dataread = dataframe.filter(dataframe("id") === id)
        if(dataread.count() > 0){
            return new User(dataread.first())
        }else{
            return User.empty
        }
    }

    // Hopefully this works.
    def read(name: String): User = {
        // return session.sql(s"SELECT * FROM users where name = $name")
        // For some reason, using the above spark sql is confusing to spark;
        // good riddance; I'd rather user programatic syntax anyway.
        val dataread = dataframe.filter(dataframe("name") === name)
        if(dataread.count() == 1){
            return new User(dataread.first())
        }else{
            return User.empty
        }
    }

    def update(id: Int, name: String, password: String, role: String): User = {
        // HACK: Just delete the row and write in a new row. That seems simple 
        // enough, but also kind of s`tupid.
        delete(id)
        return create(name, password, role, id)

    }

    def delete(id: Int) = {
        dataframe = dataframe.filter(!dataframe("id").isin(id))
        save()
    }

}

case class User(name: String, password: String, role: String, var id: Long = -1L){
    // Hmmm. This assums that we're only putting in one user at a time.
    // This may go awry.
    def this(row: org.apache.spark.sql.Row) = {
        this(
            row.getAs[String]("name"),
            row.getAs[String]("password"),
            row.getAs[String]("role"),
            row.getAs[Long]("id")
        )
    }

    if(id == -1L){
        id = session.sql(s"SELECT MAX(id) FROM users").first.getLong(0) + 1
    }

    def dataframe(): org.apache.spark.sql.DataFrame = {
        return DB.session.createDataFrame(
            Seq((id, name, password, role)))
        .toDF("id", "name", "passowrd", "role")
    }

    def notfound(): Boolean = {
        return id == -1       
    }

    def found(): Boolean = {
        return !notfound()
    }
}