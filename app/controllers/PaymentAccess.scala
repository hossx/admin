package controllers

/**
 * Created by chenxi on 8/29/14.
 */


import play.api._
import play.api.mvc._
import akka.util.Timeout
import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory
import akka.actor._
import akka.cluster.Cluster
import com.coinport.bitway._
import akka.event.LoggingReceive
import play.api.Logger

object PaymentAccess{
  val defaultAkkaConfig = "akka.conf"
  val akkaConfigProp = System.getProperty("akka.config")
  val akkaConfigResource = if (akkaConfigProp != null) akkaConfigProp else defaultAkkaConfig

  Logger.info("=" * 20 + "  Akka config  " + "=" * 20)
  Logger.info("  conf/" + akkaConfigResource)
  Logger.info("=" * 55)

  val config = ConfigFactory.load(akkaConfigResource)
  val domainName = config.getString("domain-name")
  val apiVersion = config.getString("api-version")
  implicit val backendSystem = ActorSystem("bitway", config)
  implicit val cluster = Cluster(backendSystem)

  val routers = new LocalRouters()
}

trait PaymentAccess { self: Controller =>
  implicit val timeout = Timeout(2 seconds)
  val routers = PaymentAccess.routers
  val apiVersion = PaymentAccess.apiVersion
  val httpPrefix = if (PaymentAccess.domainName.isEmpty) "http://localhost:9000" else s"http://${PaymentAccess.domainName}"
  val wsPrefix = if (PaymentAccess.domainName.isEmpty) "ws://localhost:9000" else s"wss://${PaymentAccess.domainName}"
}