package com.holmesprocessing.analytics.relationship

import java.io.File

import org.apache.spark.{SparkConf, SparkContext}
import com.typesafe.config.ConfigFactory

object SparkConfig {

  val config = ConfigFactory.parseFile(new File("./config/relationship.conf"))

  val hosts = "hosts"
  val username = "username"
  val password = "password"
  val keyspace = "keyspace"
  val analytics_knowledge_base = "analytics_knowledge_base"
  val analytics_mv_knowledge_base_by_feature = "analytics_mv_knowledge_base_by_feature"
  val analytics_primary_relationships = "analytics_primary_relationships"
  val results = "results"
  val results_meta = "results_meta"
  val results_data = "results_data"
  val objects_table = "objects_table"

  val appName = "relationship"
  val master = "localhost"

  val sparkconf = new SparkConf(true)
    .set("spark.cassandra.connection.host", hosts)
    .set("spark.cassandra.auth.username", username)
    .set("spark.cassandra.auth.password", password)

  val sc = new SparkContext(master, appName, sparkconf)
}
