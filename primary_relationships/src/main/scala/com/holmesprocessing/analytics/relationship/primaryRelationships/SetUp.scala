package com.holmesprocessing.analytics.relationship.primaryRelationships

import com.holmesprocessing.analytics.relationship.knowledgeBase.HelperMethods._
import com.holmesprocessing.analytics.relationship.SparkConfig._
import play.api.libs.json.Json
import com.datastax.spark.connector._

/*This file contains intermediate methods for generating Primary Relationships.*/
object SetUp {

  //find all primary relationships based on the pehash feature
  def generate_pehash(id: String) : Array[Byte] = {

    val feature_type = "pehash"

    val query = sc.cassandraTable(keyspace, analytics_knowledge_base).where("object_id = ?",id).map(x=> (x.get[String]("feature_type"),x.get[Array[Byte]]("feature_value"),x.get[java.util.UUID]("timestamp"))).filter(x=> x._1==feature_type).sortBy(x=> x._3,false).cache()

    if (!query.isEmpty()) {
      val value = query.map(x=> decompress(x._2)).first
      val search = sc.cassandraTable(keyspace, analytics_mv_knowledge_base_by_feature).where("feature_type =?",feature_type).map(x=> (x.get[String]("object_id"), decompress(x.get[Array[Byte]]("feature_value")), x.get[java.util.UUID]("timestamp"))).filter(x=> x._1 != id).sortBy(x=> (x._1, x._3),false)
        .filter(x=> x._2==value).map(x=> (x._1,x._3)).reduceByKey((ts1,ts2) => if (ts1.compareTo(ts2) ==1) ts1 else ts2).map(x=> Json.obj("sha256"-> x._1, "weight" -> 0.5))
      return compress(Json.toJson(search.collect).toString().getBytes)
    }
    else {
      return compress(Array[Byte](0))
    }
  }

  //find all primary relationships based on the imphash feature
  def generate_imphash(id: String) : Array[Byte] = {

    val feature_type = "imphash"

    val query = sc.cassandraTable(keyspace, analytics_knowledge_base).where("object_id = ?",id).map(x=> (x.get[String]("feature_type"),x.get[Array[Byte]]("feature_value"),x.get[java.util.UUID]("timestamp"))).filter(x=> x._1==feature_type).sortBy(x=> x._3,false).cache()

    if (!query.isEmpty()) {
      val value = query.map(x=> decompress(x._2)).first
      val search = sc.cassandraTable(keyspace, analytics_mv_knowledge_base_by_feature).where("feature_type =?",feature_type).map(x=> (x.get[String]("object_id"), decompress(x.get[Array[Byte]]("feature_value")), x.get[java.util.UUID]("timestamp"))).filter(x=> x._1 != id).sortBy(x=> (x._1, x._3),false)
        .filter(x=> x._2==value).map(x=> (x._1,x._3)).reduceByKey((ts1,ts2) => if (ts1.compareTo(ts2) ==1) ts1 else ts2).map(x=> Json.obj("sha256"-> x._1, "weight" -> 0.5))
      return compress(Json.toJson(search.collect).toString().getBytes)
    }
    else {
      return compress(Array[Byte](0))
    }
  }

  //find all primary relationships based on the signature feature
  def generate_signature(id: String) : Array[Byte] = {

    val feature_type = "binary_signature"

    val query = sc.cassandraTable(keyspace, analytics_knowledge_base).where("object_id = ?",id).map(x=> (x.get[String]("feature_type"),x.get[Array[Byte]]("feature_value"),x.get[java.util.UUID]("timestamp"))).filter(x=> x._1==feature_type).sortBy(x=> x._3,false).cache()

    if (!query.isEmpty()) {
      val value = query.map(x=> decompress(x._2)).first
      val search = sc.cassandraTable(keyspace, analytics_mv_knowledge_base_by_feature).where("feature_type =?",feature_type).map(x=> (x.get[String]("object_id"), decompress(x.get[Array[Byte]]("feature_value")), x.get[java.util.UUID]("timestamp"))).filter(x=> x._1 != id).sortBy(x=> (x._1, x._3),false)
        .filter(x=> x._2==value).map(x=> (x._1,x._3)).reduceByKey((ts1,ts2) => if (ts1.compareTo(ts2) ==1) ts1 else ts2).map(x=> Json.obj("sha256"-> x._1, "weight" -> 1.0))
      return compress(Json.toJson(search.collect).toString().getBytes)
    }
    else {
      return compress(Array[Byte](0))
    }
  }

  //find all primary relationships based on the yara feature
  def generate_yara(id:String) : Array[Byte] = {

    val feature_type = "yara_rules"

    val query = sc.cassandraTable(keyspace, analytics_knowledge_base).where("object_id = ?",id).map(x=> (x.get[String]("feature_type"),x.get[Array[Byte]]("feature_value"),x.get[java.util.UUID]("timestamp"))).filter(x=> x._1==feature_type).sortBy(x=> x._3,false).cache()

    if (!query.isEmpty) {
      val value = query.map(x=> decompress(x._2)).first
      val search = sc.cassandraTable(keyspace, analytics_mv_knowledge_base_by_feature).where("feature_type =?",feature_type).map(x=> (x.get[String]("object_id"), decompress(x.get[Array[Byte]]("feature_value")), x.get[java.util.UUID]("timestamp"))).filter(x=> x._1 != id).sortBy(x=> (x._1, x._3),false)
        .filter(x=> score(x._2, value) > 0).map(x=> ((x._1, score(x._2, value)),x._3)).reduceByKey((ts1,ts2) => if (ts1.compareTo(ts2) ==1) ts1 else ts2).map(x=> Json.obj("sha256"-> x._1._1, "weight" -> x._1._2))
      return compress(Json.toJson(search.collect).toString().getBytes)
    }
    else {

      return compress(Array[Byte](0))
    }
  }

  //find all primary relationships based on the cuckoo domains feature
  def generate_cuckoo(id:String) : Array[Byte] = {

    val feature_type = "CUCKOO"

    val query = sc.cassandraTable(keyspace, analytics_knowledge_base).where("object_id = ?",id).map(x=> (x.get[String]("feature_type"),x.get[Array[Byte]]("feature_value"),x.get[java.util.UUID]("timestamp"))).filter(x=> x._1==feature_type).sortBy(x=> x._3,false).cache()

    if (!query.isEmpty) {
      val value = query.map(x=> decompress(x._2)).first
      val search = sc.cassandraTable(keyspace, analytics_mv_knowledge_base_by_feature).where("feature_type =?",feature_type).map(x=> (x.get[String]("object_id"), decompress(x.get[Array[Byte]]("feature_value")), x.get[java.util.UUID]("timestamp"))).filter(x=> x._1 != id).sortBy(x=> (x._1, x._3),false)
        .filter(x=> score(x._2, value) > 0).map(x=> ((x._1, score(x._2, value)),x._3)).reduceByKey((ts1,ts2) => if (ts1.compareTo(ts2) ==1) ts1 else ts2).map(x=> Json.obj("sha256"-> x._1._1, "weight" -> x._1._2))
      return compress(Json.toJson(search.collect).toString().getBytes)
    }
    else {
      return compress(Array[Byte](0))
    }

  }


}
