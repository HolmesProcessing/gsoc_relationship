case class VT_sample_label_rdd_class(sha256:String, label:Array[Double])
case class peinfo_final_array_rdd_class(sha256:String, array_results :Array[Double])
case class objdump_binaray_final_array_rdd_class(sha256: String, array_results: Array[Double])
import PreProcessingConfig._
import Array.concat

val VT_sample_label_rdd = spark.read.format("parquet").load(VT_sample_label_file).rdd.map(row => new VT_sample_label_rdd_class(row(0).toString,row(1).asInstanceOf[Seq[Double]].toArray))
val VT_sample_label_rdd_before_join = VT_sample_label_rdd.map(x=>(x.sha256,x.label))

val peinfo_final_array_rdd = spark.read.format("parquet").load(peinfo_final_array_file).rdd.map(row => new peinfo_final_array_rdd_class(row(0).toString,row(1).asInstanceOf[Seq[Double]].toArray))
val peinfo_final_array_rdd_before_join = peinfo_final_array_rdd.map(x=>(x.sha256,x.array_results))

val objdump_binaray_final_array_rdd = spark.read.format("parquet").load(objdump_binaray_final_array_file).rdd.map(row => new objdump_binaray_final_array_rdd_class(row(0).toString,row(1).asInstanceOf[Seq[Double]].toArray))
val objdump_binaray_final_array_rdd_before_join = objdump_binaray_final_array_rdd.map(x=>(x.sha256,x.array_results))

val VT_peinfo_join_rdd = VT_sample_label_rdd_before_join.join(peinfo_final_array_rdd_before_join).map(x=> (x._1, x._2))
val VT_peinfo_objdump_join_rdd = VT_peinfo_join_rdd.join(objdump_binaray_final_array_rdd_before_join).map(x=> (x._1, (x._2._1._1, concat(x._2._1._2,x._2._2))))

VT_peinfo_objdump_join_rdd.toDF().write.format("parquet").save(VT_peinfo_objdump_join_file)
