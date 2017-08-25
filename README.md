# GSoC: Holmes Automated Malware Relationships

## Introduction

![GitHub Logo](/images/architecture.png)
*Figure 1: System Architecture*

#### Overview

The purpose of this project is to develop a system capable of automatically identifying and managing the relationships
between malware objects (IP addresses, Domains, Executables, Files etc). This system uses the analytic results as
generated and stored by Holmes-Totem and Holmes-Totem-Dynamic. The goals are:

1. Define the malware attributes necessary for relationship detection through querying.
2. Implement Machine Learning algorithms for relationship detection.
3. Implement an algorithm for aggregating and scoring the final relationships.
4. Visualize relationships for the client.

This system performs malware relationship detection and scoring by using a range of queries and ML algorithms.
We implement and optimize some existing and will use new ML algorithms in order to ensure accuracy and efficiency. The whole relationship detection and rating process goes through two stages and at the end the user receives a visual
representation of the generated final relationships.

#### Technology

This project uses Apache Spark 2.0 to perform its analytics and Apache Cassandra 3.10 for its backend. The Machine Learning
components of the project use the library [TensorFlowOnSpark](https://github.com/yahoo/TensorFlowOnSpark).

#### Defining and Modeling Relationships

[This](https://github.com/HolmesProcessing/gsoc_relationship/tree/master/primary_relationships) is the Spark Application responsible for generating
the Knowledge Base and Primary Relationships for this project. This application performs batch analytics and storage. To run the application, first run the script in python to set up the necessary tables in Cassandra. Before running the application, please enter your configurations directly in the ```SparkConfig.scala``` file.

***Warning***: Right now, I haven't found a way to read the typesafe config file during the spark routine. This problem should be fixed in the near future.

To test the application, create a fat jar by running ``` sbt assembly ```. Afterwards, you can run the application on your spark cluster using the following command:

```> /path/to/spark-submit --class com.holmesprocessing.analytics.relationship.PrimaryRelationshipsApplication relationship-assembly-1.0.jar```

#### Neural Network and Visualization
##### Neural Network
###### Preprocessing
[This](https://github.com/HolmesProcessing/gsoc_relationship/tree/master/ml/pre-processing) is responsible for preprocessing the data. You just need to copy the code to the Zeppelin and run the code in this sequence that you can get the formatted and labelled data that can be used in the neural network.

```
PreProcessingConfig.scala
get_VT_signatures.scala
get_labels_from_VT_signatures.scala
get_features_from_peinfo.scala
get_features_from_objdump.scala
get_labels_features_by_join.scala
```
In addition, you also need to update the `x86Opcodes` and `prefix_suffix.txt` file to the hdfs. The storage path in hdfs is the root path of the current user. You can also change the path in the configuration file.

###### Neural network
[This](https://github.com/hi-T0day/gsoc_relationship/tree/master/ml/NN) is the neural network algorithm for the data. At first, you should install the TensorflowOnSpark based the model of your Spark as the reference [here](https://github.com/yahoo/TensorFlowOnSpark/wiki).

***Warning***: The TensorFlowOnSpark cannot run well in the HDP YARN I was using because of the wrong configuration which I could not solve. While, in my local Standalone Spark Cluster I was able to run it with these versions: 
`Spark version: “spark-2.1.0-bin-hadoop2.6” `
`Hadoop version: “hadoop-2.6.2"`

Executing the following command and setting the `FolderPath` to you file path, you can run the neural network algorithm.
```
spark-submit \
 			--master spark://master:7077 --py-files /FolderPath/NN_dist.py \
 			--conf spark.cores.max=3 --conf spark.task.cpus=1 \
			--conf spark.executorEnv.JAVA_HOME="$JAVA_HOME" --conf spark.executorEnv.LD_LIBRARY_PATH="${JAVA_HOME}/jre/lib/amd64/server" \
			--conf spark.executorEnv.CLASSPATH="$($HADOOP_HOME/bin/hadoop classpath --glob):${CLASSPATH}" \
			/FolderPath/NN_spark.py
```
In the configuration file, there are three modes to choice, default is the `mode = train`.

value              | description
-------------------| ----------------------------
train     | train mode is used to train a model for the neural network.
inference | inference mode is used to show the accuracy of the model by labelled data. You should do this before you have trained a model.
predict   | predict mode is used to predict the data without the label. You should do this before you have trained a model. 

##### Visualization
###### Generate the random final score
[This](https://github.com/hi-T0day/gsoc_relationship/tree/master/visualization/final_relationships) is an alternative to generating final score data. You just need copy the code in `generate_json.js` to the website [Generate_Json](http://beta.json-generator.com/), and then click the generate button, you will get the JSON file.

###### Visualizaton page
[This](http://120.77.40.25/newd3/index.html) is the web page to visualize the final score relationship.
Here are the features of the web page:

features           | description
-------------------| ----------------------------
Threshold View | When we set a threshold score, those nodes whose scores are greater than the threshold are displayed.
Artefact View | When we click the artefact’s name, the nodes with this feature are added to ( or deleted from ) the map.
Quick View | This view shows a histogram that displays the number of nodes in different segments of scores.
Collapsible | When we click on the branch node, the branch node’s leaf nodes are folded and also the branch node is filled with steelblue.
Zoomable | We can change the map scale by mouse wheel and also translate the map by dragging.
Tooltip | When we move the mouse to the node, the tooltip shows useful information.
