name := "tshrdlu"

version := "0.1.6-SNAPSHOT"

organization := "edu.utexas"

scalaVersion := "2.10.1"

crossPaths := false

retrieveManaged := true

resolvers ++= Seq(
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  "Local Maven Repository" at Path.userHome.asFile.toURI.toURL + ".m2/repository"
)

libraryDependencies ++= Seq(
  "org.twitter4j" % "twitter4j-core" % "3.0.3",
  "org.twitter4j" % "twitter4j-stream" % "3.0.3",
  "org.scalanlp" % "nak" % "1.1.1",
  "com.typesafe.akka" %% "akka-actor" % "2.1.2",
  "commons-codec" % "commons-codec" % "1.7",
  "org.apache.lucene" % "lucene-core" % "4.2.0",
  "org.apache.lucene" % "lucene-analyzers-common" % "4.2.0",
  "org.apache.lucene" % "lucene-queryparser" % "4.2.0",
  "de.bwaldvogel" % "liblinear" % "1.92"
)

initialCommands in console := "import tshrdlu.repl._\nReplBot.setup\nprintln(\"***\")\nprintln(\"To activate repliers, call ReplBot.loadReplier(name). See Bot.scala for names.\")\nprintln(\"call ReplBot.sendTweet(text) or its overloads.\")\nprintln(\"***\")"