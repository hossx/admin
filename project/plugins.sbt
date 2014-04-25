// Comment to get more information during initialization
logLevel := Level.Warn

resolvers ++= Seq(
  "Nexus Snapshots" at "http://192.168.0.105:8081/nexus/content/groups/public/",
  Resolver.sonatypeRepo("snapshots")
)

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3-SNAPSHOT")
