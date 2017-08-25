import com.typesafe.config._
import java.io._
val conf = ConfigFactory.parseFile(new File("../config/pre-processing.conf"))
object PreProcessingConfig extends Serializable{
  val kmeans_cluster_number = conf.getInt("preprocess.kmeans_cluster_number");
  val keyspace = conf.getString("preprocess.keyspace")
  val service_name_table = conf.getString("preprocess.service_name_table")
  val sha256_table = conf.getString("preprocess.sha256_table")
  val VT_signatures_prefix_suffix_file = conf.getString("preprocess.VT_signatures_prefix_suffix_file")
  val objdump_x86Opcodes_file = conf.getString("preprocess.objdump_x86Opcodes_file")

  val VT_sample_signatures_final_array_file = conf.getString("preprocess.VT_sample_signatures_final_array_file")
  val VT_sample_label_file = conf.getString("preprocess.VT_sample_label_file")
  val peinfo_final_array_file = conf.getString("preprocess.peinfo_final_array_file")
  val objdump_binaray_final_array_file = conf.getString("preprocess.objdump_binaray_final_array_file")
  val VT_peinfo_objdump_join_file = conf.getString("preprocess.VT_peinfo_objdump_join_file")
}
