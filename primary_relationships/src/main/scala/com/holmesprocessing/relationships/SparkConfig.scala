package com.holmesprocessing.relationships

import org.apache.spark.{SparkConf, SparkContext}
import com.typesafe.config.ConfigFactory

object SparkConfig {

  private val config = ConfigFactory.load("relationships")

  val hosts = "127.0.0.1"//config.getString("cassandra.hosts")
  val username = "user"//config.getString("cassandra.username")
  val password = "pwd"//config.getString("cassandra.password")
  val keyspace = "keyspace"//config.getString("cassandra.keyspace")
  val knowledge_base_table = "knowledge_base_table"//config.getString("cassandra.knowledge_base_table")
  val mv_feature_type_table = "mv_feature_type_table"//config.getString("cassandra.mv_feature_type_table")
  val primary_relationships_table = "primary_relationships_table"//config.getString("cassandra.primary_relationships_table")
  val results = "results"//config.getString("cassandra.results")
  val results_meta_by_sha256 = "results_meta_by_sha256"//config.getString("cassandra.results_meta_by_sha256")
  val results_data_by_sha256 = "results_data_by_sha256"//config.getString("cassandra.results_data_by_sha256")
  val objects_table = "objects_table"

  val appName = "relationships"//config.getString("spark.appName")
  val master = "localhost"//config.getString("spark.master")

  val sparkconf = new SparkConf(true)
    .set("spark.cassandra.connection.host", hosts )
    .set("spark.cassandra.auth.username", username)
    .set("spark.cassandra.auth.password", password)

  val sc = new SparkContext(master, appName, sparkconf)
}
