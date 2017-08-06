import com.datastax.spark.connector._
import play.api.libs.json.Json
import java.io.{ByteArrayOutputStream, ByteArrayInputStream}
import java.util.zip.{GZIPOutputStream, GZIPInputStream}
import PreProcessingConfig._

case class VT_results_by_service_name_class(service_name: String, sha256: String)
case class VT_results_by_sha256_class(sha256: String, service_name: String, results: Array[Byte] )
case class VT_join_results_class(sha256: String, service_name: String, results: String)
case class VT_sample_signatures_initial_seq_rdd_class(sha256: String, seq_results: Seq[String])
case class VT_sample_signatures_final_array_rdd_class(sha256:String, array_results:Array[Double])

def unzip(x: Array[Byte]) : String = {		
    val inputStream = new GZIPInputStream(new ByteArrayInputStream(x))
    val output = scala.io.Source.fromInputStream(inputStream).mkString
    return output
}
def deleteNumberInSampleSignatures(x: String): Boolean = {
    val regex = "[0-9]".r
    return regex.findFirstIn(x).isEmpty
}

val VT_results_by_service_name_meta = sc.cassandraTable[VT_results_by_service_name_class](keyspace,service_name_table).where("service_name=?","virustotal")
val VT_results_by_service_name_rdd = VT_results_by_service_name_meta.keyBy(x=> (x.sha256,x.service_name))
val VT_results_by_sha256_meta = sc.cassandraTable[VT_results_by_sha256_class](keyspace,sha256_table)
val VT_results_by_sha256_rdd = VT_results_by_sha256_meta.keyBy(x => (x.sha256,x.service_name))
val VT_join_results = VT_results_by_service_name_rdd.join(VT_results_by_sha256_rdd).map(x => (new VT_join_results_class(x._1._1,x._1._2, unzip(x._2._2.results)))).distinct().cache()
val sample_signatures_rdd = VT_join_results.flatMap(x=>Json.parse(x.results) \ "scans" \\ "result").map(x=>Json.stringify(x)).filter( x=> !(x == "null"))
val sample_signatures_split_rdd = sample_signatures_rdd.flatMap(x=>x.replaceAll("""["]""","").replaceAll("""\![a-zA-Z0-9\s\+]+""","").replaceAll("""@[a-zA-Z0-9\s\+]+""","").replaceAll("""~[a-zA-Z0-9\s\+]+""","").replaceAll("""[\(|\[|{][a-zA-Z0-9\s\+]*[\)|\]|}]""","").replaceAll("""(\.|\!|\:|\_|\-|\\|/|\[|\])"""," ").split(" ")).filter(x=>(x.size>3)).filter(x=>deleteNumberInSampleSignatures(x)).map(x=>x.toLowerCase())
val signatures_prefix_rdd = sc.textFile(VT_signatures_prefix_suffix_file).map(x=>x.toLowerCase())
val family_signatures_subtract_rdd = sample_signatures_split_rdd.subtract(signatures_prefix_rdd)
val family_signatures_sorted_rdd = sc.parallelize(family_signatures_subtract_rdd.countByValue().toSeq).filter(x=>(x._2>50)).sortBy(x=>x._2,false)
val family_signatures_list = family_signatures_sorted_rdd.keys.collect().toList
val VT_sample_signatures_rdd = VT_join_results.map(x=>(x.sha256,(Json.parse(x.results) \ "scans" \\ "result").map(_.toString).filter( s => !(s== "null")).flatMap(x=>x.replaceAll("""["]""","").replaceAll("""\![a-zA-Z0-9\s\+]+""","").replaceAll("""@[a-zA-Z0-9\s\+]+""","").replaceAll("""~[a-zA-Z0-9\s\+]+""","").replaceAll("""[\(|\[|{][a-zA-Z0-9\s\+]*[\)|\]|}]""","").replaceAll("""(\.|\!|\:|\_|\-|\\|/|\[|\])"""," ").split(" ")).filter(x=>(x.size>3)).filter(x=>deleteNumberInSampleSignatures(x)).map(x=>x.toLowerCase())))
val  VT_sample_signatures_initial_seq_rdd = VT_sample_signatures_rdd.map(x=>new VT_sample_signatures_initial_seq_rdd_class(x._1, x._2))

implicit def bool2int(b:Boolean) = if (b) 1 else 0
def findAllInFamilySignatures(sample_signatures_seq : Seq[String]) : Array[Double] ={
    val forlist = for (family <- family_signatures_list) yield {
        (sample_signatures_seq.contains(family):Int).toDouble
    }
    return forlist.toArray
}

val VT_sample_signatures_final_array_rdd = VT_sample_signatures_initial_seq_rdd.map(x=>new VT_sample_signatures_final_array_rdd_class(x.sha256,findAllInFamilySignatures(x.seq_results)))
VT_sample_signatures_final_array_rdd.toDF().write.format("parquet").save(VT_sample_signatures_final_array_file)
