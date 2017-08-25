import com.datastax.spark.connector._
import play.api.libs.json.Json
import play.api.libs.json._
import java.io.{ByteArrayOutputStream, ByteArrayInputStream}
import java.util.zip.{GZIPOutputStream, GZIPInputStream}
import Array.concat
import org.apache.spark.sql.types._
import org.apache.spark.ml.linalg.SQLDataTypes.VectorType 
import org.apache.spark.ml.linalg._
import org.apache.spark.sql.Row
import org.apache.spark.ml.feature.MinMaxScaler
import org.apache.spark.ml.linalg.DenseVector
import PreProcessingConfig._

case class peinfo_results_by_service_name_class(service_name: String, sha256: String)
case class peinfo_results_by_sha256_class(sha256: String, service_name: String, results: Array[Byte])
case class peinfo_join_results_class(sha256: String, service_name: String, results: String)
case class peinfo_int_final_array_rdd_class(sha256: String, array_results: Array[Double])
case class peinfo_binaray_final_array_rdd_class(sha256:String, array_results :Array[Double])
case class peinfo_final_array_rdd_class(sha256:String, array_results: Array[Double])

def unzip(x: Array[Byte]) : String = {      
    val inputStream = new GZIPInputStream(new ByteArrayInputStream(x))
    val output = scala.io.Source.fromInputStream(inputStream).mkString
    return output
}
def findAllIntinpeinfo( peinfo_json_results : JsLookupResult, time: Double): Array[Double]= {
    val entropy = peinfo_json_results \\ "entropy" ; val virt_address = peinfo_json_results \\ "virt_address"; val virt_size = peinfo_json_results \\ "virt_size"; val size = peinfo_json_results \\ "size";
    var i= 0; var List  = Array.iterate(0.0,17)(a=>a*0)
    for (k <- ( peinfo_json_results \\ "section_name")){
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
    return List.toArray
}

val peinfo_results_by_service_name_meta = sc.cassandraTable[peinfo_results_by_service_name_class](keyspace,service_name_table).where("service_name=?","peinfo")
val peinfo_results_by_service_name_rdd = peinfo_results_by_service_name_meta.keyBy(x=> (x.sha256,x.service_name))
val peinfo_results_by_sha256_meta = sc.cassandraTable[peinfo_results_by_sha256_class](keyspace,sha256_table)
val peinfo_results_by_sha256_rdd = peinfo_results_by_sha256_meta.keyBy(x => (x.sha256,x.service_name))
val peinfo_join_results = peinfo_results_by_service_name_rdd.join(peinfo_results_by_sha256_rdd).map(x=> (new peinfo_join_results_class(x._1._1,x._1._2, unzip(x._2._2.results)))).distinct().cache()

val peinfo_int_final_array_rdd = peinfo_join_results.map(x=>(x.sha256,(Json.parse(x.results) \ "pe_sections"),{if ((Json.parse(x.results) \ "timestamp").isInstanceOf[JsUndefined]) 0.0 else (Json.parse(x.results) \ "timestamp" \\ "timestamp")(0).as[Double]})).filter(x=> !x._2.isInstanceOf[JsUndefined]).map(x=>new  peinfo_int_final_array_rdd_class(x._1,findAllIntinpeinfo(x._2,x._3)))

val peinfo_dllfunction_list= peinfo_join_results.map(x=>Json.parse(x.results) \ "imports").filter(x=> !x.isInstanceOf[JsUndefined]).flatMap(x=>x.as[List[Map[String, String]]].map(x=>(x("dll")+"."+x("function")))).toDF("func_name").groupBy("func_name").count.sort(desc("count")).filter("count > 10000").rdd.map(r => r.getString(0)).collect().toList
implicit def bool2int(b:Boolean) = if (b) 1 else 0
def findAllBininpeinfo_dllfunction(peinfo_dllfunction : Seq[String]) : Array[Double] ={
    val forlist = for (family <- peinfo_dllfunction_list) yield {
        (peinfo_dllfunction.contains(family):Int).toDouble
    }
    return (forlist).toArray
}
val List502 = Array.iterate(0.0,502)(a=>0.0)
val peinfo_binaray_final_array_rdd = peinfo_join_results.map(x=>(x.sha256,(Json.parse(x.results) \ "imports"))).map(x=>new  peinfo_binaray_final_array_rdd_class(x._1,{if (x._2.isInstanceOf[JsUndefined]) List502 else findAllBininpeinfo_dllfunction(x._2.as[Seq[Map[String, String]]].map(x=>(x("dll")+"."+x("function"))))}))

val peinfo_int_final_array_rdd_before_join = peinfo_int_final_array_rdd.map(x=>(x.sha256,x.array_results))
val peinfo_binaray_final_array_rdd_before_join = peinfo_binaray_final_array_rdd.map(x=>(x.sha256,x.array_results))
val peinfo_array_rdd_by_join = peinfo_int_final_array_rdd_before_join.join(peinfo_binaray_final_array_rdd_before_join).map(x=> (x._1,concat(x._2._1,x._2._2)))
val peinfo_final_array_rdd = peinfo_array_rdd_by_join.map(x=>new peinfo_final_array_rdd_class(x._1,x._2))

val peinfo_schema = new StructType().add("sha256", StringType).add("peinfo",VectorType)
val peinfo_vector_rdd = peinfo_final_array_rdd.map(x=>(x.sha256,Vectors.dense(x.array_results)))
val peinfo_vector_rowrdd = peinfo_vector_rdd.map(p => Row(p._1,p._2))
val peinfo_vector_dataframe = spark.createDataFrame(peinfo_vector_rowrdd, peinfo_schema)
val peinfo_scaler = new MinMaxScaler()
  .setInputCol("peinfo")
  .setOutputCol("scaled_peinfo")
val peinfo_scalerModel = peinfo_scaler.fit(peinfo_vector_dataframe)
val peinfo_scaledData_df = peinfo_scalerModel.transform(peinfo_vector_dataframe)
val peinfo_scaledData_rdd = peinfo_scaledData_df.select("sha256","scaled_peinfo").rdd.map(row=>(row.getAs[String]("sha256"),row.getAs[DenseVector]("scaled_peinfo"))).map(x=>new peinfo_final_array_rdd_class(x._1,x._2.toArray))
peinfo_scaledData_rdd.toDF().write.format("parquet").save(peinfo_final_array_file)
