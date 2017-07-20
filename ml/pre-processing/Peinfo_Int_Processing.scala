import com.datastax.spark.connector._
import play.api.libs.json.Json
import play.api.libs.json._
import java.io.{ByteArrayOutputStream, ByteArrayInputStream}
import java.util.zip.{GZIPOutputStream, GZIPInputStream}

case class results_by_service_name(service_name: String, sha256: String)
case class results_by_sha256(sha256: String, service_name: String, results: Array[Byte])
case class FinalResults(sha256: String, service_name: String, results: String)
case class Pe_int_matrix_final_class(sha256: String, matrix: Seq[Double])

def unzip(x: Array[Byte]) : String = {		
    val inputStream = new GZIPInputStream(new ByteArrayInputStream(x))
    val output = scala.io.Source.fromInputStream(inputStream).mkString
    return output
}
val List17 = Array.iterate(0.0,17)(a=>a*0)
def findInfoInPe_section( Jsanswer : JsLookupResult, time: Double): Seq[Double]= {
    val entropy = Jsanswer \\ "entropy" ; val virt_address = Jsanswer \\ "virt_address"; val virt_size = Jsanswer \\ "virt_size"; val size = Jsanswer \\ "size";
    var i= 0; var List  = List17
    for (k <- ( Jsanswer \\ "section_name")){
        k.as[String] match {
            case ".text\u0000\u0000\u0000" => { List(0)=entropy(i).as[Double]; List(1)=Integer.parseInt(virt_address(i).as[String].substring(2), 16).toDouble; List(2)=virt_size(i).as[Double]; List(3)=size(i).as[Double] }
            case ".data\u0000\u0000\u0000" => { List(4)=entropy(i).as[Double]; List(5)=Integer.parseInt(virt_address(i).as[String].substring(2), 16).toDouble; List(6)=virt_size(i).as[Double]; List(7)=size(i).as[Double] }
            case ".rsrc\u0000\u0000\u0000" => { List(8)=entropy(i).as[Double]; List(9)=Integer.parseInt(virt_address(i).as[String].substring(2), 16).toDouble; List(10)=virt_size(i).as[Double]; List(11)=size(i).as[Double] }
            case ".rdata\u0000\u0000" => { List(12)=entropy(i).as[Double]; List(13)=Integer.parseInt(virt_address(i).as[String].substring(2), 16).toDouble; List(14)=virt_size(i).as[Double]; List(15)=size(i).as[Double] }
            case other => {}
        }
        i = i + 1
    }
    List(16)= time
    return List
}

val res_by_service = sc.cassandraTable[results_by_service_name]("gsoc3","results_meta_by_service_name").where("service_name=?","peinfo")
val res_by_service_rdd = res_by_service.keyBy(x=> (x.sha256,x.service_name))
val res_by_sha = sc.cassandraTable[results_by_sha256]("gsoc3","results_data_by_sha256")
val res_by_sha_rdd = res_by_sha.keyBy(x => (x.sha256,x.service_name))
val results = res_by_service_rdd.join(res_by_sha_rdd).map(x=> (new FinalResults(x._1._1,x._1._2, unzip(x._2._2.results)))).distinct()
val Pe_int_matrix_final = results.map(x=>(x.sha256,(Json.parse(x.results) \ "pe_sections"),{if ((Json.parse(x.results) \ "timestamp").isInstanceOf[JsUndefined]) 0.0 else (Json.parse(x.results) \ "timestamp" \\ "timestamp")(0).as[Double]})).filter(x=> !x._2.isInstanceOf[JsUndefined]).map(x=>new Pe_int_matrix_final_class(x._1,findInfoInPe_section(x._2,x._3)))
Pe_int_matrix_final.toDF().write.format("parquet").save("./Pe_int_matrix_final.parquet")
