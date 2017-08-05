import SparkConfig._

import com.datastax.spark.connector._
import com.datastax.driver.core.utils.UUIDs
import java.util.UUID
import scala.collection.mutable.ArrayBuffer
import play.api.libs.json.Json
import play.api.libs.json.Reads._
import java.util.zip.{GZIPOutputStream, GZIPInputStream}
import java.io.ByteArrayInputStream



// case classes for querying Cassandra
case class results_meta_by_sha256(sha256: String, service_name: String, object_category: Set[String], source_tags: Set[String], tags: Set[String], user_id: String, id: java.util.UUID)
case class results_data_by_sha256(sha256: String, service_name: String, id: java.util.UUID, results: Array[Byte])
case class DataResults(sha256: String, service_name: String, id: java.util.UUID, object_category: Set[String], source_tags: Set[String], tags: Set[String], user_id: String, results: String)

//case class for storing the knowledge base
case class Knowledge_Base(object_id: String, feature_type: String, feature_value: String, timestamp: java.util.UUID)

//helper functions
def unzip(x: Array[Byte]) : String = {		
    val inputStream = new GZIPInputStream(new ByteArrayInputStream(x))
    val output = scala.io.Source.fromInputStream(inputStream).mkString
    return output
}


def get_digitalsig(result: String) : String = {

    val x= (Json.parse(result) \ "version_info" \\ "value").map(_.toString)
    val y = (Json.parse(result) \ "version_info" \\ "key").map(_.toString)
    val sig = y.zip(x).filter(x=>(x._1).contains("Signature")).map(_._2)

    if (sig.isEmpty) {
      return "NONE"
    }
    return sig.head.replaceAll("""["]""","")
}

def get_mic_sig(result : String) : Seq[String] = {
    val signature = (Json.parse(result) \ "scans" \ "Microsoft" \ "result").toString
    if (signature == "null"){
      return Seq("")
    }
    return signature.replaceAll("""["]""","").split("""[;,.:/]""").toSeq
}

def get_sym_sig(result : String) : Seq[String] = {
    val signature = (Json.parse(result) \ "scans" \ "Symantec" \ "result").toString
    if (signature == "null"){
      return Seq("")
    }
    return signature.replaceAll("""["]""","").split("""[;,.:/]""").toSeq
}

def get_ks_sig(result : String) : Seq[String] = {
    val signature = (Json.parse(result) \ "scans" \ "Kaspersky" \ "result").toString
    if (signature == "null"){
      return Seq("")
    }
    return signature.replaceAll("""["]""","").split("""[;,.:/]""").toSeq
}

def get_vinfo(result: String) : String = { //Seq[(String, String)] = {

    val x= (Json.parse(result) \ "version_info" \\ "value").map(_.toString)
    val y = (Json.parse(result) \ "version_info" \\ "key").map(_.toString)
    val vinfo = y.zip(x)
    return vinfo.mkString
}

def get_cuckoo_url(result: String) : String = {
    
    val domains = (Json.parse(result) \\ "Data").map(x=> (x \ "arguments" \ "url")).filter(y => y.isDefined).map(_.as[String]).mkString(",")
    return domains
}

def run_query(id:String, keyspace: String, tablename: String) = {
    
    val res_meta_by_sha256 = sc.cassandraTable[results_meta_by_sha256]("gsoc3","results_meta_by_sha256").where("sha256=?",id).cache()
    val res_meta_by_sha256_rdd = res_meta_by_sha256.keyBy(x=> (x.sha256,x.service_name, x.id))
    val res_data_by_sha256 = sc.cassandraTable[results_data_by_sha256]("gsoc3","results_data_by_sha256").cache()
    val res_data_by_sha256_rdd = res_data_by_sha256.keyBy(x => (x.sha256,x.service_name,x.id))
    val results = res_meta_by_sha256_rdd.join(res_data_by_sha256_rdd).map(x=> (new DataResults(x._1._1, x._1._2, x._1._3, x._2._1.object_category, x._2._1.source_tags, x._2._1.tags, x._2._1.user_id , unzip(x._2._2.results)))).cache()
    val knowledge_base = ArrayBuffer[Knowledge_Base]()
    
    //PEINFO
    val peinfo_res = results.filter(x=> x.service_name == "peinfo").sortBy(_.id).collect
    if (!peinfo_res.isEmpty) {
    
        //Imphash
        val imphash = (Json.parse(peinfo_res(0).results) \ "imphash").asOpt[String].getOrElse("Undefined")
        if (!imphash.equals("Undefined")) {
            val rel_k = new Knowledge_Base(peinfo_res(0).sha256, "imphash", imphash, UUIDs.timeBased())
            knowledge_base.append(rel_k)
        }
        
        //Pehash
        val pehash = (Json.parse(peinfo_res(0).results) \ "pehash").asOpt[String].getOrElse("Undefined")
        if (!pehash.equals("Undefined")) {
            val rel_k = new Knowledge_Base(peinfo_res(0).sha256, "pehash", pehash, UUIDs.timeBased())
            knowledge_base.append(rel_k)
        }
        
        //DigitalSignature
        val digitsig = get_digitalsig(peinfo_res(0).results)
        if(!digitsig.equals("NONE")){
            val rel_k = new Knowledge_Base(peinfo_res(0).sha256, "signature", digitsig, UUIDs.timeBased())
            knowledge_base.append(rel_k)
        }
    }
    
    //YARA
    val yara_res = results.filter(x=> x.service_name == "yara").sortBy(_.id).collect

    if(!yara_res.isEmpty){
    
        val yara_rules = yara_res(0).results.replaceAll("[^a-zA-Z0-9.,_]","").replace("yara","").replace("rule:","")//.mapValues(_.split(",")).toSeq  <------ stored as String, not as List
        val rel_k = new Knowledge_Base(yara_res(0).sha256, "yara_rules", yara_rules, UUIDs.timeBased())
        knowledge_base.append(rel_k)
    }

    //VIRUSTOTAL
    val vt_res = results.filter(x=> x.service_name == "virustotal").sortBy(_.id).collect

    if(!vt_res.isEmpty){
    
        val vt_sigs = (get_mic_sig(vt_res(0).results)++get_sym_sig(vt_res(0).results)++get_ks_sig(vt_res(0).results)).mkString
        val rel_k = new Knowledge_Base(vt_res(0).sha256, "vt_signatures", vt_sigs, UUIDs.timeBased())
        knowledge_base.append(rel_k)
    }

    //CUCKOO
    val cuckoo_res = results.filter(x=> x.service_name == "CUCKOO").sortBy(_.id).collect

    if(!cuckoo_res.isEmpty){
    
        val domains = get_cuckoo_url(cuckoo_res(0).results)
        if (domains != "") {
            val rel_k = new Knowledge_Base(cuckoo_res(0).sha256, "called_domains", domains, UUIDs.timeBased())
            knowledge_base.append(rel_k)
        }
    }
    
    sc.parallelize(knowledge_base).saveToCassandra(args(0),args(1), SomeColumns("object_id", "feature_type", "feature_value", "timestamp"))
    println("Done!")
}



object Spark_KB_Setup {
	def main(args:Array[String]) {
		
		val all_unique_artefacts = sc.cassandraTable(args(0),"objects").select("sha256").map(x=> x.get[String]("sha256")).distinct().collect
		var y = ""
		for( y <- all_unique_artefacts){
    			run_query(y, args(0),args(1))
    		}
	}
}

