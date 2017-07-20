import com.datastax.spark.connector._
import play.api.libs.json.Json

case class holmes_results_by_service_name(service_name: String, sha256: String, results: String)
case class gsoc3_results_by_sha256(sha256: String)
case class VT_results_class(sha256: String, service_name: String, results: String)
case class VT_matrix_initial_class(sha256: String, VTsplit: Seq[String])
case class VT_matrix_final_class(sha256:String, matrix:List[Int])

def noNumber(x: String): Boolean = {
    val regex = "[0-9]".r
    return regex.findFirstIn(x).isEmpty
}

val res_meta_holmes_VT = sc.cassandraTable[holmes_results_by_service_name]("holmes","results").where("service_name=?","virustotal")
val res_meta_holmes_VT_rdd = res_meta_holmes_VT.keyBy(x=> x.sha256)
val res_meta_gsoc3_sha256 = sc.cassandraTable[gsoc3_results_by_sha256]("gsoc3","results_data_by_sha256")
val res_meta_gsoc3_sha256_rdd = res_meta_gsoc3_sha256.keyBy(x => x.sha256).distinct()
val VT_results = res_meta_gsoc3_sha256_rdd.join(res_meta_holmes_VT_rdd).map( x => new VT_results_class(x._1,x._2._2.service_name,x._2._2.results)).cache()

val signatures_list = VT_results.flatMap(x=>Json.parse(x.results) \ "scans" \\ "result").map(x=>Json.stringify(x)).filter( x=> !(x == "null"))
val signatures_split = signatures_list.flatMap(x=>x.replaceAll("""["]""","").replaceAll("""\![a-zA-Z0-9\s\+]+""","").replaceAll("""@[a-zA-Z0-9\s\+]+""","").replaceAll("""~[a-zA-Z0-9\s\+]+""","").replaceAll("""[\(|\[|{][a-zA-Z0-9\s\+]*[\)|\]|}]""","").replaceAll("""(\.|\!|\:|\_|\-|\\|/|\[|\])"""," ").split(" ")).filter(x=>(x.size>3)).filter(x=>noNumber(x)).map(x=>x.toLowerCase())
val prefix = sc.textFile("./prefix.txt").map(x=>x.toLowerCase())
val family_signature = signatures_split.subtract(prefix)
val family_names = sc.parallelize(family_signature.countByValue().toSeq).filter(x=>(x._2>10)).sortBy(x=>x._2,false)
val seqfamily = family_names.keys.collect().toList
val sha256_VT = VT_results.map(x=>(x.sha256,(Json.parse(x.results) \ "scans" \\ "result").map(_.toString).filter( s => !(s== "null")).flatMap(x=>x.replaceAll("""["]""","").replaceAll("""\![a-zA-Z0-9\s\+]+""","").replaceAll("""@[a-zA-Z0-9\s\+]+""","").replaceAll("""~[a-zA-Z0-9\s\+]+""","").replaceAll("""[\(|\[|{][a-zA-Z0-9\s\+]*[\)|\]|}]""","").replaceAll("""(\.|\!|\:|\_|\-|\\|/|\[|\])"""," ").split(" ")).filter(x=>(x.size>3)).filter(x=>noNumber(x)).map(x=>x.toLowerCase())))
val VT_matrix_initial = sha256_VT.map(x=>new VT_matrix_initial_class(x._1, x._2))

implicit def bool2int(b:Boolean) = if (b) 1 else 0
def findAllInFamily(VT : Seq[String]) : List[Int] ={
    val forlist = for (family <- seqfamily) yield {
        VT.contains(family):Int
    }
    return forlist
}
val VT_matrix_final = VT_matrix_initial.map(x=>new VT_matrix_final_class(x.sha256,findAllInFamily(x.VTsplit)))
VT_matrix_final.toDF().write.format("parquet").save("./VT_matrix_final.parquet")
