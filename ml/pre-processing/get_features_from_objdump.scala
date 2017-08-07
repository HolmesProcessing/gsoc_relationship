import com.datastax.spark.connector._
import play.api.libs.json.Json
import play.api.libs.json._
import java.io.{ByteArrayOutputStream, ByteArrayInputStream}
import java.util.zip.{GZIPOutputStream, GZIPInputStream}
import PreProcessingConfig._

case class objdump_results_by_service_name_class(service_name: String, sha256: String)
case class objdump_results_by_sha256_class(sha256: String, service_name: String, results: Array[Byte])
case class objdump_join_results_class(sha256: String, service_name: String, results: String)
case class objdump_binaray_final_array_rdd_class(sha256: String, array_results: Array[Double])
 
val objdump_main_list = sc.textFile(objdump_x86Opcodes_file).collect.toList
def unzip(x: Array[Byte]) : String = {		
    val inputStream = new GZIPInputStream(new ByteArrayInputStream(x))
    val output = scala.io.Source.fromInputStream(inputStream).mkString
    return output
}
def combineAllObjdumpInOne( malwarelist :Seq[play.api.libs.json.JsValue]) : List[String] ={
    if (malwarelist(0).toString() == "null") return List("null")
    var begin = malwarelist(0).as[List[String]]
    for (i <- 1 to (malwarelist.size-1)){
        if (malwarelist(i).toString() == "null") begin = begin
        else begin = begin ::: malwarelist(i).as[List[String]]
    }
    return  begin
}
def convertToList( malwarelist :Seq[play.api.libs.json.JsValue]) : List[String] = {
    if (malwarelist(0).toString() == "null") return List("null")
    else {
        return malwarelist(0).as[List[String]]
    } 
    
}
def findAllBininobjdump_main_list(malware :List[String]) : Array[Double] ={
    if (malware == List("null")) return (List.fill(10000)(0.0)).toArray
    else {
        val forlist = for ( one  <- malware ) yield {
            objdump_main_list.indexOf(one) + 1.0
        }
        if (forlist.size < 10000){
            return  (List.concat(forlist,List.fill(10000-forlist.size)(0.0))).toArray
        }
        else return forlist.toArray
    }
}

val objdump_results_by_service_name_meta = sc.cassandraTable[objdump_results_by_service_name_class](keyspace,service_name_table).where("service_name=?","objdump")
val objdump_results_by_service_name_rdd = objdump_results_by_service_name_meta.keyBy(x=> (x.sha256,x.service_name))
val objdump_results_by_sha256_meta = sc.cassandraTable[objdump_results_by_sha256_class](keyspace,sha256_table)
val objdump_results_by_sha256_rdd = objdump_results_by_sha256_meta.keyBy(x => (x.sha256,x.service_name))
val objdump_join_results = objdump_results_by_service_name_rdd.join(objdump_results_by_sha256_rdd).map(x=> (new objdump_join_results_class(x._1._1,x._1._2, unzip(x._2._2.results)))).distinct()
val objdump_binaray_final_array_rdd = objdump_join_results.map(x=>(x.sha256,(Json.parse(x.results) \\ "opcodes"))).filter(x=> (x._2.size > 0)).map(x=>(x._1,if ( x._2.size == 1 ) convertToList(x._2) else combineAllObjdumpInOne(x._2))).map(x=>(x._1,findAllBininobjdump_main_list(x._2)))
objdump_binaray_final_array_rdd.toDF().write.format("parquet").save(objdump_binaray_final_array_file)
