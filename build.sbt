import sbt.Keys._

name := """coinport-admin"""

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.10.4"

lazy val root = (project in file("."))
    .enablePlugins(PlayScala)
    .enablePlugins(SbtTwirl)


resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots")
)

libraryDependencies ++= {
  val akkaVersion = "2.3.3"
  val coinexVersion = "1.1.32-SNAPSHOT"
  Seq(
    "com.typesafe.akka"           %% "akka-remote"                      % akkaVersion,
    "com.typesafe.akka"           %% "akka-cluster"                     % akkaVersion,
    "com.typesafe.akka"           %% "akka-slf4j"                       % akkaVersion,
    "com.typesafe.akka"           %% "akka-contrib"                     % akkaVersion,
    "com.typesafe.akka"           %% "akka-testkit"                     % akkaVersion,
    //"com.coinport"         %% "bitway-client"       % "0.0.9-SNAPSHOT",
    "org.json4s"                  %% "json4s-native"                    % "3.2.8",
    "org.json4s"                  %% "json4s-ext"                       % "3.2.8",
    "com.github.tototoshi"        %% "play-json4s-native"               % "0.2.0",
    "com.github.tototoshi"        %% "play-json4s-test-native"          % "0.2.0" % "test",
    "com.coinport"                %% "coinex-client"                    % coinexVersion,
    "com.typesafe.akka"           %% "akka-remote"                      % "2.3.4",
    "com.twilio.sdk"              %  "twilio-java-sdk"                  % "3.4.1",
    "net.debasishg"               %% "redisclient"                      % "2.12",
    // "com.octo.captcha"            %  "jcaptcha"                         % "1.0",
    // "com.cloopen"                 %  "restsdk"                          % "2.6.1"
    "cn.bestwu"                   %  "ccp-rest"                         % "2.7"
  )
}

// Imaging is rename to filters. So exclude imaging and import filters
libraryDependencies += ("com.octo.captcha" % "jcaptcha" % "1.0")
    .exclude("com.jhlabs", "imaging")

libraryDependencies += ("com.jhlabs" % "filters" % "2.0.235-1")

javaOptions in Test += "-Djava.library.path=/Users/chm/lib/jnotify-lib-0.94"
