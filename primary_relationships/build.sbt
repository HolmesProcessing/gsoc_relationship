name := "relationship"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "org.apache.commons" % "commons-lang3" % "3.5",
  "com.datastax.spark" % "spark-cassandra-connector_2.11" % "2.0.2",
  "com.typesafe.play" % "play-json_2.11" % "2.6.0-M5" exclude("com.fasterxml.jackson.core","jackson-databind"),
  "org.apache.spark" %% "spark-core" % "2.2.0" % "provided",
  "org.apache.spark" %% "spark-sql" % "2.2.0" % "provided",
  "com.typesafe" % "config" % "1.3.1"
)
