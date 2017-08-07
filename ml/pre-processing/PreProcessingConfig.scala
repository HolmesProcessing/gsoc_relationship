object PreProcessingConfig {

  val keyspacekeyspace = "gsoc3"
  val service_name_table = "results_meta_by_service_name"
  val sha256_table = "results_data_by_sha256"
  val VT_signatures_prefix_suffix_file = "./prefix.txt"
  val objdump_x86Opcodes_file = "./x86Opcodes"

  val VT_sample_signatures_final_array_file = "./VT_sample_signatures_final_array.parquet" 
  val VT_sample_label_file = "./VT_sample_label.parquet"
  val peinfo_final_array_file = "./peinfo_final_array.parquet"
  val objdump_binaray_final_array_file = "./objdump_binaray_final_array.parquet"
  val VT_peinfo_objdump_join_file = "./VT_peinfo_objdump_join.parquet"
}
