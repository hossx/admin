/**
 * Copyright 2014 Coinport Inc. All Rights Reserved.
 * Author: c@coinport.com (Chao Ma)
 */

package services.edm

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.event.LoggingReceive

import com.mongodb.casbah.{ MongoConnection, MongoURI, WriteConcern }
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.query.Imports._

import scala.concurrent.duration.DurationInt
import scala.concurrent.Await

import com.coinport.coinex.api.service.AccountService
import com.coinport.coinex.api.service.TransferService
import com.coinport.coinex.api.service.UserService
import com.coinport.coinex.data.Implicits._
import com.coinport.coinex.data._
import org.bson.types.ObjectId

class EdmChecker(host: String, port: Int) extends Actor with ActorLogging {

  private case class FetchEdm()

  private val mongoUri = MongoURI(s"mongodb://${host}:${port}/edm")
  private val edmCollection = MongoConnection(mongoUri)(mongoUri.database.get)("edms")

  override def preStart = {
    super.preStart
    scheduleEdm()
  }

  def receive = LoggingReceive {
    case FetchEdm =>
      processPendingEdm()
      scheduleEdm()
  }

  private def scheduleEdm() {
    context.system.scheduler.scheduleOnce(3 seconds, self, FetchEdm)(context.system.dispatcher)
  }

  private def processPendingEdm() {
    for (item <- edmCollection.find(MongoDBObject("s" -> "PENDING"))) {
      try {
        val id = item.get("_id").asInstanceOf[ObjectId]
        val email = item.get("e").asInstanceOf[String]
        val tpl = item.get("tn").asInstanceOf[String]
        edmCollection.update(MongoDBObject("_id" -> id), $set("s" -> "PROCESSING"), false, false, WriteConcern.Safe)

        val result = Await.result(UserService.sendEdm(email, tpl), 5 seconds)
        if (result.success) {
          edmCollection.update(MongoDBObject("_id" -> id), $set("s" -> "SENT"), false, false, WriteConcern.Safe)
        } else {
          edmCollection.update(MongoDBObject("_id" -> id), $set("s" -> "FAIL"), false, false, WriteConcern.Safe)
        }
      } catch {
        case e: Throwable => println(e)
      }
      Thread.sleep(1000)
    }
  }
}
