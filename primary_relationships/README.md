This is the Spark Application responsible for generating
the Knowledge Base and Primary Relationships for this 
project. To run the generation routine you can create a 
fat jar by running 
``` sbt assembly ```. 

Adapt the content of ```./config/relationship.conf``` 
before assembling the code. Afterwards, you can run the 
application on your spark cluster using:

```> /path/to/spark-submit --class com.holmesprocessing.analytics.relationship.PrimaryRelationshipsApplication relationship-assembly-1.0.jar```

***Warning***: right now the typesafe config file does not
work correctly in the application. This problem will be fixed 
in the near future. If you want to run the application for
testing purposes anyway, please alter the content of the 
```SparkConfig.scala``` file.