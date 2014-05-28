name := """coinport-admin"""

version := "1.0.0-SNAPSHOT"

resolvers ++= Seq(
  "Nexus Snapshots" at "http://192.168.0.105:8081/nexus/content/groups/public/",
  Resolver.sonatypeRepo("snapshots")
)

libraryDependencies ++= {
  val akkaVersion = "2.3.0"
  Seq(
    "com.typesafe.akka"           %% "akka-remote"                      % akkaVersion,
    "com.typesafe.akka"           %% "akka-cluster"                     % akkaVersion,
    "com.typesafe.akka"           %% "akka-slf4j"                       % akkaVersion,
    "com.typesafe.akka"           %% "akka-contrib"                     % akkaVersion,
    "com.typesafe.akka"           %% "akka-testkit"                     % akkaVersion,
    "org.json4s" %% "json4s-native" % "3.2.8",
    "org.json4s" %% "json4s-ext" % "3.2.8",
    "com.github.tototoshi" %% "play-json4s-native" % "0.2.0",
    "com.github.tototoshi" %% "play-json4s-test-native" % "0.2.0" % "test",
    "com.coinport" %% "coinex-client" % "1.1.16-SNAPSHOT"
  )
}

play.Project.playScalaSettings

