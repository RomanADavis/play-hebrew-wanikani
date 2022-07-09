package models

import org.apache.spark.sql.Row
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.types.{StringType, StructField, StructType, LongType}

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

    var dataframe = session.read.schema(schema).json(json_path)
    dataframe.cache()
    dataframe.createOrReplaceTempView("letters")

    def all(): org.apache.spark.sql.DataFrame = {
        // SELECT DISTINCT is a hack here: somehow, when I add a row to my table,
        // it gets added twice to the dataframe (but only once as a JSON);
        // should probably hunt down the bug later.
        return session.sql("SELECT DISTINCT * FROM letters ORDER BY id")
    }

    // Hopefully this works.
    def read(name: String): org.apache.spark.sql.DataFrame = {
        // return session.sql(s"SELECT * FROM users where name = $name")
        // For some reason, using the above spark sql is confusing to spark;
        // good riddance; I'd rather user programatic syntax anyway.
        return dataframe.filter(dataframe("name") === name)
    }

    def save() = {
        dataframe.write.mode("overwrite").json(json_path)
        dataframe.cache()
    }

    def create(string: String, glyph: String, name: String, meaning: String, rationale: String): Letter = {
        all()
        val letter: Letter = new Letter(string, glyph, name, meaning, rationale)

        letter.dataframe().show()
        dataframe = dataframe.union(letter.dataframe())
        dataframe.createOrReplaceTempView("letter")
        
        save()
        return letter
    }
}

case class Letter(string: String, glyph: String, name: String, meaning: String, rationale: String){
    def dataframe(): org.apache.spark.sql.DataFrame = {
        return DB.session.createDataFrame(
            Seq((string, glyph, name, meaning, rationale)))
        .toDF("letter", "glyph", "name", "meaning", "rationale")
    }
}