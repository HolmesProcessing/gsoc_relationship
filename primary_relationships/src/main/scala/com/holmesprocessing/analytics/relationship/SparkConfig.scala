package com.holmesprocessing.analytics.relationship

import java.io.File

import org.apache.spark.{SparkConf, SparkContext}
import com.typesafe.config.ConfigFactory

object SparkConfig {

  val config = ConfigFactory.parseFile(new File("./config/relationship.conf"))

  val hosts = config.getString("cassandra.hosts")
  val username = config.getString("cassandra.username")
  val password = config.getString("cassandra.password")
  val keyspace = config.getString("cassandra.keyspace")
  val analytics_knowledge_base = config.getString("cassandra.analytics_knowledge_base")
  val analytics_mv_knowledge_base_by_feature = config.getString("cassandra.analytics_mv_knowledge_base_by_feature")
  val analytics_primary_relationships = config.getString("cassandra.analytics_primary_relationships")
  val results = config.getString("cassandra.results")
  val results_meta = config.getString("cassandra.results_meta")
  val results_data = config.getString("cassandra.results_data")
  val objects_table = config.getString("cassandra.objects_table")

  val appName = config.getString("spark.appName")
  val master = config.getString("spark.master")

  val sparkconf = new SparkConf(true)
    .set("spark.cassandra.connection.host", hosts)
    .set("spark.cassandra.auth.username", username)
    .set("spark.cassandra.auth.password", password)

  val sc = new SparkContext(master, appName, sparkconf)
}
