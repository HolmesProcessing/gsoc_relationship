package com.holmesprocessing.relationships

import com.holmesprocessing.relationships.primaryRelationships.PrimaryRelationshipsGenerator
import com.holmesprocessing.relationships.knowledgeBase.KnowledgeBaseGenerator
import com.holmesprocessing.relationships.SparkConfig._

import com.datastax.driver.core.utils.UUIDs
import com.datastax.spark.connector._

object PrimaryRelationshipsApplication {

  def main(args: Array[String]): Unit = {

    //setup a knowledge base for a test run
    val kb_batch = sc.cassandraTable(keyspace,objects_table).select("sha256").map(x=> x.get[String]("sha256")).distinct().take(100).toList
    KnowledgeBaseGenerator.run(kb_batch)

    //setup primary relationships for a test run.
    val pk_batch = sc.cassandraTable(keyspace,knowledge_base_table).select("object_id").map(x=> x.get[String]("object_id")).first
    PrimaryRelationshipsGenerator.run(pk_batch)

    sc.stop()
  }

}
