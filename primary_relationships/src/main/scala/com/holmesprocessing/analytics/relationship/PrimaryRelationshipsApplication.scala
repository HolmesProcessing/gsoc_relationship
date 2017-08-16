package com.holmesprocessing.analytics.relationship

import com.holmesprocessing.analytics.relationship.primaryRelationships.PrimaryRelationshipsGenerator
import com.holmesprocessing.analytics.relationship.knowledgeBase.KnowledgeBaseGenerator
import com.holmesprocessing.analytics.relationship.SparkConfig._

import java.util.UUID
import com.datastax.spark.connector._

object PrimaryRelationshipsApplication {

  def main(args: Array[String]): Unit = {

    //setup a knowledge base for a test run
    val kb_batch = sc.cassandraTable(keyspace,objects_table).select("sha256").map(x=> x.get[String]("sha256")).distinct().take(100).toList
    KnowledgeBaseGenerator.run(kb_batch)

    //setup primary relationships for a test run.
    val pk_batch = sc.cassandraTable(keyspace,analytics_knowledge_base).select("object_id").map(x=> x.get[String]("object_id")).first
    PrimaryRelationshipsGenerator.run(pk_batch)

    sc.stop()
  }

}
