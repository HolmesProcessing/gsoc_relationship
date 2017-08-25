# This is used to 1/train the model from the labeled data 2/predict the label of the labeled data. 3/predicte the data without label. 
# 1) train the model from the labeled data
# command: 	spark-submit \
# 			--master spark://master:7077 --py-files /FolderPath/NN_dist.py \
# 			--conf spark.cores.max=3 --conf spark.task.cpus=1 \
#			--conf spark.executorEnv.JAVA_HOME="$JAVA_HOME" --conf spark.executorEnv.LD_LIBRARY_PATH="${JAVA_HOME}/jre/lib/amd64/server" \
#			--conf spark.executorEnv.CLASSPATH="$($HADOOP_HOME/bin/hadoop classpath --glob):${CLASSPATH}" \
#			/FolderPath/NN_spark.py

# 2) predict the label of the labeled data
# Output Example -- Label: 0, Prediction: 0
# command: 	spark-submit \
# 			--master spark://master:7077 --py-files /FolderPath/NN_dist.py \
# 			--conf spark.cores.max=3 --conf spark.task.cpus=1 \
#			--conf spark.executorEnv.JAVA_HOME="$JAVA_HOME" --conf spark.executorEnv.LD_LIBRARY_PATH="${JAVA_HOME}/jre/lib/amd64/server" \
#			--conf spark.executorEnv.CLASSPATH="$($HADOOP_HOME/bin/hadoop classpath --glob):${CLASSPATH}" \
#			/FolderPath/NN_spark.py

# 3) predicte the data without label. 
# Output Example -- Sha256: 4f913360e14c5da32ff2d2f1cf1a1e788448d1d1b7eed1810da5fac4cf80aca1, Prediction: 0
# command: 	spark-submit \
# 			--master spark://master:7077 --py-files /FolderPath/NN_dist.py \
# 			--conf spark.cores.max=3 --conf spark.task.cpus=1 \
#			--conf spark.executorEnv.JAVA_HOME="$JAVA_HOME" --conf spark.executorEnv.LD_LIBRARY_PATH="${JAVA_HOME}/jre/lib/amd64/server" \
#			--conf spark.executorEnv.CLASSPATH="$($HADOOP_HOME/bin/hadoop classpath --glob):${CLASSPATH}" \
#			/FolderPath/NN_spark.py

# Tips: Code is based on the TensorflowOnSpark(TFoS) mnist example.

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

from pyspark.context import SparkContext
from pyspark.conf import SparkConf
from pyspark.sql import SQLContext

import os
import numpy
import sys
import tensorflow as tf
import threading
import time
from datetime import datetime

from tensorflowonspark import TFCluster
import NN_dist
from pyhocon import ConfigFactory 

sc = SparkContext(conf=SparkConf().setAppName("NN_spark"))
sqlContext = SQLContext(sc)
executors = sc._conf.get("spark.executor.instances")
num_executors = int(executors) if executors is not None else 1
num_ps = 1

class argsClass:
    def __init__(self):
		conf = ConfigFactory.parse_file("../config/ml.conf")
		self.mode = conf["NN.mode"]     	
		self.input_file = conf["NN.input_file"]
		self.model = conf["NN.model"]
		self.output = conf["NN.output"]
		self.batch_size = conf["NN.batch_size"]  
		self.epochs = conf["NN.epochs"]
		self.cluster_size = conf["NN.cluster_size"]
		self.steps = conf["NN.steps"]
args = argsClass()

print("{0} ===== Start".format(datetime.now().isoformat()))

if args.mode == "train" or args.mode == "inference":
	datafile = sqlContext.read.format("parquet").load(args.input_file)
	labelRDD= datafile.rdd.map(lambda row: row._2._1)
	featureRDD = datafile.rdd.map(lambda row: row._2._2)
	dataRDD = featureRDD.zip(labelRDD)
else:
	datafile = sqlContext.read.format("parquet").load(args.input_file)
	sha256RDD= datafile.rdd.map(lambda row: row._1)
	featureRDD = datafile.rdd.map(lambda row: row._2._2)
	dataRDD = featureRDD.zip(sha256RDD)	

cluster = TFCluster.run(sc, NN_dist.map_fun, args, args.cluster_size, num_ps, False, TFCluster.InputMode.SPARK)
if args.mode == "train":
  cluster.train(dataRDD, args.epochs)
else:
  labelRDD = cluster.inference(dataRDD)
  labelRDD.saveAsTextFile(args.output)
cluster.shutdown()

print("{0} ===== Stop".format(datetime.now().isoformat()))
