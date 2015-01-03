/**
 * Copyright 2014 Coinport Inc. All Rights Reserved.
 * Author: c@coinport.com (Chao Ma)
 */

import akka.actor._
import akka.cluster.Cluster
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import play.api._
import play.api.Play.current
import services.transfer.GoocTransfer

object Global extends GlobalSettings {

  Logger.info("Deploy trade system....")

  val configPath = if (System.getProperty("gooc.config") != null) System.getProperty("gooc.config") else "gooc.conf"

  val config = ConfigFactory.load(configPath)
  implicit var system: ActorSystem = null

  override def onStart(app: Application) {
    Logger.info("Application start...")
    system = ActorSystem("quant", config)
    system.actorOf(Props(new GoocTransfer(config.getString("akka.mongo.host"), config.getInt("akka.mongo.port"))),
      "gooc_transfer")
  }

  override def onStop(app: Application) {
    Logger.info("Application shutdown...")
    system.shutdown
    system.awaitTermination
  }
}
