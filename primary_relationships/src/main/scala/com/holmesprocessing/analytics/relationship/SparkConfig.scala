package com.holmesprocessing.analytics.relationship

import org.apache.spark.{SparkConf, SparkContext}
import com.typesafe.config.ConfigFactory

object SparkConfig {

  val config = ConfigFactory.load("relationship")

  val hosts = "127.0.0.1"//config.getString("cassandra.hosts")
  val username = "user"//config.getString("cassandra.username")
  val password = "pwd"//config.getString("cassandra.password")
  val keyspace = "keyspace"//config.getString("cassandra.keyspace")
  val analytics_knowledge_base = "analytics_knowledge_base"//config.getString("cassandra.analytics_knowledge_base")
  val analytics_mv_knowledge_base_by_feature = "analytics_mv_knowledge_base_by_feature"//config.getString("cassandra.analytics_mv_knowledge_base_by_feature")
  val analytics_primary_relationships = "analytics_primary_relationships"//config.getString("cassandra.analytics_primary_relationships")
  val results = "results"//config.getString("cassandra.results")
  val results_meta = "results_meta"//config.getString("cassandra.results_meta")
  val results_data = "results_data"//config.getString("cassandra.results_data")
  val objects_table = "objects_table"//config.getString("cassandra.objects_table")

  val appName = "relationship"//config.getString("spark.appName")
  val master = "localhost"//config.getString("spark.master")

  val sparkconf = new SparkConf(true)
    .set("spark.cassandra.connection.host", hosts)
    .set("spark.cassandra.auth.username", username)
    .set("spark.cassandra.auth.password", password)

  val sc = new SparkContext(master, appName, sparkconf)
}
