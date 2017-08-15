package com.holmesprocessing.relationships.primaryRelationships

import com.holmesprocessing.relationships.models.Record
import com.holmesprocessing.relationships.knowledgeBase.HelperMethods._
import com.holmesprocessing.relationships.SparkConfig._
import com.holmesprocessing.relationships.primaryRelationships.SetUp._
import com.datastax.driver.core.utils.UUIDs
import com.datastax.spark.connector._

object PrimaryRelationshipsGenerator {


  def run(id: String) = {

    val pehash = generate_pehash(id)
    val imphash = generate_imphash(id)
    val signature = generate_signature(id)
    val yara_rules = generate_yara(id)
    val domains = generate_cuckoo(id)

    sc.parallelize(Seq(Record(id, pehash, imphash, signature, yara_rules, domains, UUIDs.timeBased()))).saveToCassandra(keyspace,primary_relationships_table, SomeColumns("object_id", "pehash", "imphash", "binary_signature","yara_rules","domain_requests", "timestamp"))

  }

}
