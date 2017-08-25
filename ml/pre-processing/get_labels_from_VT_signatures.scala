import org.apache.spark.mllib.feature.PCA
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.clustering.KMeans
import PreProcessingConfig._

case class VT_sample_label_rdd_class(sha256:String, label:Array[Double])

def OnehotEncode(number : Double): Array[Double]={
    var Listnew = Array.iterate(0.0,kmeans_cluster_number)(a=>0.0)
    Listnew(number.toInt)=1
    return Listnew
}

val VT_sample_signatures_final_array_rdd = spark.read.format("parquet").load(VT_sample_signatures_final_array_file).rdd.map(row => new VT_sample_signatures_final_array_rdd_class(row(0).toString,row(1).asInstanceOf[Seq[Double]].toArray))
val VT_sample_signatures_with_sha_rddvector = VT_sample_signatures_final_array_rdd.map(x=>(x.sha256,Vectors.dense(x.array_results)))
val VT_sample_signatures_rddvector = VT_sample_signatures_with_sha_rddvector.map(x=>x._2)
val KMeans_Model = KMeans.train(VT_sample_signatures_rddvector,kmeans_cluster_number,30,2)
val VT_sample_signatures_label_with_sha_rdd = VT_sample_signatures_with_sha_rddvector.map(x=>(x._1,KMeans_Model.predict(x._2)))
val VT_sample_label_rdd = VT_sample_signatures_label_with_sha_rdd.map(x=>new VT_sample_label_rdd_class(x._1, OnehotEncode(x._2.toDouble)))

VT_sample_label_rdd.toDF().write.format("parquet").save(VT_sample_label_file)
