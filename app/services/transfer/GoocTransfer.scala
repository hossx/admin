/**
 * Copyright 2014 Coinport Inc. All Rights Reserved.
 * Author: c@coinport.com (Chao Ma)
 */

package services.transfer

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
import com.coinport.coinex.data.Implicits._
import com.coinport.coinex.data._

class GoocTransfer(host: String, port: Int) extends Actor with ActorLogging {

  private case class FetchTransferItem()

  private val mongoUri = MongoURI(s"mongodb://${host}:${port}/gooc")
  private val txCollection = MongoConnection(mongoUri)(mongoUri.database.get)("dTxs")

  override def preStart = {
    super.preStart
    scheduleFetchTransferItem()
  }

  def receive = LoggingReceive {
    case FetchTransferItem =>
      processPendingRequest()
      scheduleFetchTransferItem()
  }

  private def scheduleFetchTransferItem() {
    context.system.scheduler.scheduleOnce(3 seconds, self, FetchTransferItem)(context.system.dispatcher)
  }

  private def processPendingRequest() {
    for (tx <- txCollection.find(MongoDBObject("cps" -> "PENDING", "ty" -> "DEPOSIT"))) {
      try {
        val id = tx.get("_id").asInstanceOf[Long]
        val uid = tx.get("c").asInstanceOf[String].replaceAll("""\s+""", "").replaceAll("\"", "").toLong
        val amount = tx.get("a").asInstanceOf[Double]
        val currency: Currency = Currency.Gooc
        println(s"processing ${id} deposit item")
        txCollection.update(MongoDBObject("_id" -> id), $set("cps" -> "PROCESSING"), false, false, WriteConcern.Safe)
        val result = Await.result(AccountService.deposit(uid, currency, amount), 5 seconds)
        if (result.success) {
          val cptxid = result.data.get.asInstanceOf[RequestTransferSucceeded].transfer.id
          txCollection.update(MongoDBObject("_id" -> id),
            $set("cps" -> "PROCESSED", "cptxid" -> cptxid, "cpuid" -> uid), false, false, WriteConcern.Safe)
          TransferService.AdminConfirmTransfer(cptxid, true)
        } else {
          txCollection.update(MongoDBObject("_id" -> id), $set("cps" -> "FAILED"), false, false, WriteConcern.Safe)
        }
      } catch {
        case e: Throwable => println(e)
      }
      Thread.sleep(1000)
    }
  }
}
