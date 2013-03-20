name := "tshrdlu"

version := "0.1.5-SNAPSHOT"

organization := "edu.utexas"

scalaVersion := "2.10.0"

crossPaths := false

retrieveManaged := true

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
 

libraryDependencies ++= Seq(
  "org.twitter4j" % "twitter4j-core" % "3.0.3",
  "org.twitter4j" % "twitter4j-stream" % "3.0.3",
  "org.scalanlp" % "nak" % "1.1.0",
  "commons-codec" % "commons-codec" % "1.7",
  "org.apache.lucene" % "lucene-core" % "4.2.0",
  "org.apache.lucene" % "lucene-analyzers-common" % "4.2.0",
  "org.apache.lucene" % "lucene-queryparser" % "4.2.0"
)
