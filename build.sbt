name := "tshrdlu"

version := "0.1.3-SNAPSHOT"

organization := "edu.utexas"

scalaVersion := "2.10.0"

crossPaths := false

retrieveManaged := true

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
 

libraryDependencies ++= Seq(
  "org.twitter4j" % "twitter4j-core" % "3.0.3",
  "org.twitter4j" % "twitter4j-stream" % "3.0.3",
  "com.typesafe.akka" %% "akka-actor" % "2.1.0",
  "org.scalanlp" % "chalk" % "1.1.1",
  "org.scalanlp" % "nak" % "1.1.0"
)
