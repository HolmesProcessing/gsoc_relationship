import org.apache.spark.SparkContext
import org.apache.spark.SparkConf


object SparkConfig {

  val keyspace = "keyspace"

  val kb_table = "object_knowledge_base_table"
  val primary_relationships = "primary_relationships_table"

  val cassandra_cluster = "localhost"

  val spark_master = "yarn-client"

  val appname = "gsoc3_primary_relationships_generator"

  val conf = new SparkConf(true)
    .set("spark.cassandra.connection.host", cassandra_cluster)
    .set("spark.cassandra.auth.username", "username")
    .set("spark.cassandra.auth.password", "password")

  val sc = new SparkContext(spark_master, appname, conf)

}
