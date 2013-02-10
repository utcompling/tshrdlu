name := "tshrdlu"

version := "0.1-SNAPSHOT"

organization := "edu.utexas"

scalaVersion := "2.10.0"

crossPaths := false

retrieveManaged := true

libraryDependencies ++= Seq(
  "org.twitter4j" % "twitter4j-core" % "3.0.3",
  "org.twitter4j" % "twitter4j-stream" % "3.0.3"
)
