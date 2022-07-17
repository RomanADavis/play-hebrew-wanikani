package models

import org.apache.spark.sql.Row
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.types.{StringType, StructField, StructType, LongType}
import org.apache.spark.sql.functions._

import org.apache.spark.sql.functions.{desc, asc}
import DB.session

// There is likely some way to remove some of the code overhead if Scala has
// some reasonable rules for Object inhertance; should look into this later.
object Letter {
    val session = DB.session
    val json_path = "hdfs://localhost:9000/user/Roman/hebrew/letters"

    val schema = StructType(
        Array(
            StructField("letter", StringType, nullable=false),
            StructField("glyph", StringType, nullable=false),
            StructField("name", StringType, nullable=false),
            StructField("meaning", StringType, nullable=false),
            StructField("rationale", StringType, nullable=false)
        )
    )

    var dataframe = session.read
        .option("multiline", true)
        .schema(schema)
        .json(json_path)

    dataframe.cache()
    dataframe.createOrReplaceTempView("letters")

    // Hack so that methods return User even when not found.
    val empty: Letter = new Letter("NULL", "NULL", "NULL", "NULL", "NULL", -1L)

    val counts = models.Root.dataframe
                    .groupBy("parent")
                    .agg(count("*")
                    .as("root count"))
        
    val root_counts = counts.select(
            counts("root count"), counts("parent").as("child-roots")
        )

    val joined = dataframe.join(
            root_counts,
            dataframe("letter") === root_counts("child-roots"),
            "left_outer"
        ).na.fill(0)

     def show() = {
        // SELECT DISTINCT is a hack here: somehow, when I add a row to my table,
        // it gets added twice to the dataframe (but only once as a JSON);
        // should probably hunt down the bug later.
        dataframe = session.sql("SELECT DISTINCT * FROM letters ORDER BY letter")
        dataframe.show()
    }

    def all(column: String = "letter", order: String = "ASC"): Array[Letter] = {
        val sorted = if(order == "ASC") joined.sort(asc(column)) else joined.sort(desc(column))
        sorted.show()

        return sorted.rdd.map(row =>
            new Letter(row)
        ).collect()
    }

    // Hopefully this works.
    def read(string: String): Letter = {
        // return session.sql(s"SELECT * FROM users where name = $name")
        // For some reason, using the above spark sql is confusing to spark;
        // good riddance; I'd rather user programatic syntax anyway.
        val dataread = joined.filter(dataframe("letter") === string)

        if(dataread.count() > 0){
            return new Letter(dataread.first())
        }else{
            return Letter.empty
        }
    }

    def save() = {
        dataframe.write.mode("overwrite").json(json_path)
        dataframe.cache()
    }

    def create(string: String, glyph: String, name: String, meaning: String, rationale: String): Letter = {
        all()
        val letter: Letter = new Letter(string, glyph, name, meaning, rationale, 0)

        letter.dataframe().show()
        dataframe = dataframe.union(letter.dataframe())
        dataframe.createOrReplaceTempView("letter")
        
        save()
        return letter
    }
}

case class Letter(string: String, glyph: String, name: String, meaning: String, 
    rationale: String, root_count: Long = 0){
    def this(row: org.apache.spark.sql.Row) = {
        this(
            row.getAs[String]("letter"),
            row.getAs[String]("glyph"),
            row.getAs[String]("name"),
            row.getAs[String]("meaning"),
            row.getAs[String]("rationale"),
            row.getAs[Long]("root count")
        )
    }

    def dataframe(): org.apache.spark.sql.DataFrame = {
        return DB.session.createDataFrame(
            Seq((string, glyph, name, meaning, rationale, root_count)))
        .toDF("letter", "glyph", "name", "meaning", "rationale", "root count")
    }
}