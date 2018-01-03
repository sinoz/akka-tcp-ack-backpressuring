name := "akka-tcp-backpressuring"

version := "1.0"

scalaVersion := "2.12.4"

libraryDependencies ++= {
  val akkaVersion = "2.5.8"
  val akkaDeps = Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion
  )

  val testDeps = Seq(
    "org.scalatest" %% "scalatest" % "3.2.0-SNAP9" % Test
  )

  akkaDeps ++ testDeps
}