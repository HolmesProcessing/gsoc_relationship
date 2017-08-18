package com.holmesprocessing.analytics.relationship.primaryRelationships

import com.holmesprocessing.analytics.relationship.models.Record
import com.holmesprocessing.analytics.relationship.SparkConfig._
import com.holmesprocessing.analytics.relationship.primaryRelationships.SetUp._
import com.datastax.driver.core.utils.UUIDs
import com.datastax.spark.connector._

object PrimaryRelationshipsGenerator {

  /*This routines finds, formats and stores all primary relationships.*/
  def run(id: String) = {

    val pehash = generate_pehash(id)
    val imphash = generate_imphash(id)
    val signature = generate_signature(id)
    val yara_rules = generate_yara(id)
    val domains = generate_cuckoo(id)

    sc.parallelize(Seq(Record(id, pehash, imphash, signature, yara_rules, domains, UUIDs.timeBased()))).saveToCassandra(keyspace, analytics_primary_relationships, SomeColumns("object_id", "pehash", "imphash", "binary_signature","yara_rules","domain_requests", "timestamp"))

  }

}
