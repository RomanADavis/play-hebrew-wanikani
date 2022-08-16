package models

import org.apache.spark.sql.Row
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.types.{StringType, StructField, StructType, LongType}
import org.apache.spark.sql.functions._

import org.apache.spark.sql.functions.{desc, asc}
import DB.session

object Root {
    val session = DB.session
    val json_path = "./json/just_roots.json"
    
    val schema = StructType(
        Array(
            StructField("root", StringType, nullable=false),
            StructField("parent", StringType, nullable=false),
            StructField("action", StringType, nullable=true),
            StructField("object", StringType, nullable=true),
            StructField("abstract", StringType, nullable=true),
            StructField("definition", StringType, nullable=true)
        )
    )

    var dataframe = session.read
        .option("multiline", true)
        .schema(schema)
        .json(json_path)
    dataframe.cache()
    dataframe.createOrReplaceTempView("root")

    // Hack so that methods return User even when not found.
    val empty: Root = new Root("NULL", "NULL", "NULL", "NULL", "NULL", "NULL", -1L)

    def show() = {
        // SELECT DISTINCT is a hack here: somehow, when I add a row to my table,
        // it gets added twice to the dataframe (but only once as a JSON);
        // should probably hunt down the bug later.
        dataframe = session.sql("SELECT DISTINCT * FROM root ORDER BY root")
        dataframe.show()
    }
    
    def root_counts(): org.apache.spark.sql.DataFrame = {
        val counts = dataframe
                        .groupBy("parent")
                        .agg(count("*")
                        .as("root count"))
        
        return counts.select(
                counts("root count"), counts("parent").as("child roots")
            ) 
    }

    def root_counts(letter: String): org.apache.spark.sql.DataFrame = {
        return root_counts().filter(dataframe("root").startsWith(letter))
    }

    def all(column: String = "letter", order: String = "ASC"): Array[Root] = {
        val sorted = if(order == "ASC") dataframe.sort(asc(column)) else dataframe.sort(desc(column))
        sorted.show()

        return sorted.rdd.map(row =>
            new Root(row)
        ).collect()
    }

    def view(letter: String = "א", column: String = "root", 
            order: String = "ASC", parent: String = ""): Array[Root] = {
        val filtered = if(parent == "") dataframe.filter(dataframe("root").startsWith(letter)) else dataframe.filter(dataframe("parent") === parent) 
        val child_counts = root_counts()
        val joined = filtered.alias("parent").join(
                child_counts, 
                filtered("root") === child_counts("child roots"),
                "left_outer"
            ).na.fill(0)
        val sorted = if(order == "ASC") joined.sort(asc(column)) else joined.sort(desc(column))
        sorted.show()
        // joined.show()
        return sorted.rdd.map(row =>
            new Root(row)
        ).collect()
    }

    def save() = {
        dataframe.createOrReplaceTempView("root")
        dataframe.write.mode("overwrite").json(json_path)
        dataframe.cache()
    }

    // "object" is a keyword; "abstract" is a keyword
    def create(root_name: String, parent: String, action: String, obj: String, abstr: String, definition: String): Root = {
        all()
        val root: Root = new Root(root_name, parent, action, obj, abstr, definition)

        root.dataframe().show()
        dataframe = dataframe.union(root.dataframe())
        
        save()
        return root
    }

    // def read(id: Int): User = {
    //     // TODO: Get this to return a user (and update the corresponding views)
    //     val dataread = dataframe.filter(dataframe("id") === id)
    //     if(dataread.count() > 0){
    //         return new User(dataread.first())
    //     }else{
    //         return User.empty
    //     }
    // }

    // Hopefully this works.
    def read(root_name: String): Root = {
        // return session.sql(s"SELECT * FROM users where name = $name")
        // For some reason, using the above spark sql is confusing to spark;
        // good riddance; I'd rather user programatic syntax anyway.
        val dataread = dataframe.filter(dataframe("root") === root_name)
        val child_counts = root_counts()
        val joined = dataread.alias("parent").join(
                child_counts, 
                dataread("root") === child_counts("child roots"),
                "left_outer"
            ).na.fill(0)

        if(joined.count() == 1){
            return new Root(joined.first())
        }else{
            return Root.empty
        }
    }

    // def update(root_name: String, parent: String, action: String, obj: String, abstr: String, definition: String): Root = {
    //     // HACK: Just delete the row and write in a new row. That seems simple 
    //     // enough, but also kind of s`tupid.
    //     delete(id)
    //     return create(root_name, parent, action, obj, abstr, definition)

    // }

    // def delete(root: String) = {
    //     dataframe = dataframe.filter(!dataframe("root").isin(root))
    //     save()
    // }

}

case class Root(root_name: String, parent: String, action: String, obj: String, 
    abstr: String, definition: String, root_count: Long = 0){
    // Hmmm. This assums that we're only putting in one user at a time.
    // This may go awry.
    def this(row: org.apache.spark.sql.Row) = {
        this(
            row.getAs[String]("root"),
            row.getAs[String]("parent"),
            row.getAs[String]("action"),
            row.getAs[String]("object"),
            row.getAs[String]("abstract"),
            row.getAs[String]("definition"),
            row.getAs[Long]("root count")
            // row.getAs[Long]("child root count")
        )
    }

    def dataframe(): org.apache.spark.sql.DataFrame = {
        return DB.session.createDataFrame(
            Seq((root_name, parent, action, obj, abstr, definition)))
        .toDF("root", "parent", "action", "object", "abstract", "definition")
    }
}