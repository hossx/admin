package services

import java.security.MessageDigest
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import akka.pattern.ask
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.mongodb.casbah.MongoURI
import com.mongodb.casbah.Imports._
import com.mongodb.util.JSON
import com.google.common.io.BaseEncoding
import org.json4s.native.Serialization.{ read, write }

import com.coinport.coinex.data._
import com.coinport.coinex.api.model._
import com.coinport.coinex.serializers._
import com.coinport.coinex.api.service.AkkaService
import com.coinport.coinex.api.service.Akka
import models.UserInfo

sealed trait SimpleMongoCollection[T <: AnyRef] {
  val coll: MongoCollection
  val DATA = "data"
  val ID = "_id"

  def extractId(obj: T): Long
  def get(id: Long): Option[T]
  def put(data: T): Unit
}

abstract class SimpleJsonMongoCollection[T <: AnyRef, S <: T](implicit man: Manifest[S]) extends SimpleMongoCollection[T] {
  import org.json4s.native.Serialization.{ read, write }
  implicit val formats = ThriftEnumJson4sSerialization.formats
  def get(id: Long) = coll.findOne(MongoDBObject(ID -> id)) map { json => read[S](json.get(DATA).toString) }
  def put(data: T) = coll += MongoDBObject(ID -> extractId(data), DATA -> JSON.parse(write(data)))

  def find(q: MongoDBObject, skip: Int, limit: Int): Seq[T] =
    coll.find(q).sort(MongoDBObject(ID -> -1)).skip(skip).limit(limit).map { json => read[S](json.get(DATA).toString) }.toSeq

  def count(q: MongoDBObject): Long = coll.count(q)
}

object UserService extends AkkaService {
  val config = Akka.config //ConfigFactory.load("akka.conf")
  val userProfiles = initUserProfiles
  val limitMax: Int = 30

  val EMAIL_FLD = userProfiles.DATA + ".email"

  private def initUserProfiles = {
    val secret = config.getString("akka.exchange.secret")
    val userManagerSecret = MHash.sha256Base64(secret + "userProcessorSecret")
    val mongoUriForViews = MongoURI(config.getString("akka.exchange.mongo-uri-for-readers"))
    val mongoForViews = MongoConnection(mongoUriForViews)
    val dbForViews = mongoForViews(mongoUriForViews.database.get)

    new SimpleJsonMongoCollection[UserProfile, UserProfile.Immutable] {
      import org.json4s.native.Serialization.read

      val coll = dbForViews("user_profiles")
      def extractId(profile: UserProfile) = profile.id

      def findAsending(q: MongoDBObject, skip: Int, limit: Int): Seq[UserProfile] = {
        coll.find(q).sort(MongoDBObject(ID -> 1)).skip(skip).limit(limit).map { json => read[UserProfile.Immutable](json.get(DATA).toString) }.toSeq
      }
    }
  }

  def searchUser(userName: String, uidFrom: Long, uidTo: Long): ApiResult = {
    val q:DBObject = if (userName != null && userName.trim.length > 0) {
      (userProfiles.ID $gte uidFrom $lte uidTo) ++ MongoDBObject(EMAIL_FLD -> (".*" + userName + ".*").r)
    } else {
      (userProfiles.ID $gte uidFrom $lte uidTo)
    }
    println(s"query cond: $q")
    val result = userProfiles.findAsending(q, 0, limitMax).map(profile2UserInfo)
    if (result != null && result.size > 0)
      ApiResult(true, 0, "", Some(result))
    else
      ApiResult(false, -1, "search result empty")
  }

  private def profile2UserInfo(profile: UserProfile): UserInfo = {
    UserInfo(profile.id, profile.email, profile.emailVerified,
      profile.mobileVerified, profile.status.toString)
  }

  private def profile2User(profile: UserProfile): User = {
    User(id = profile.id, email = profile.email, password = null,
      status = profile.status)
  }

  def suspendUser(uid: Long): Future[ApiResult] = {
    val command = DoSuspendUser(uid)
    backend ? command map {
      case result: SuspendUserResult =>
        result.userProfile match {
          case Some(profile) =>
            ApiResult(true, 0, "", Some(profile2UserInfo(profile)))
          case None =>
            ApiResult(false, ErrorCode.UserNotExist.value, "用户不存在", None)
        }
      case e =>
        ApiResult(false, -1, e.toString)
    }
  }

  def resumeUser(uid: Long): Future[ApiResult] = {
    val command = DoResumeUser(uid)
    backend ? command map {
      case result: ResumeUserResult =>
        result.userProfile match {
          case Some(profile) =>
            ApiResult(true, 0, "", Some(profile2UserInfo(profile)))
          case None =>
            ApiResult(false, ErrorCode.UserNotExist.value, "用户不存在", None)
        }
      case e =>
        ApiResult(false, -1, e.toString)
    }
  }
}

object MHash {
  def sha256Base64(str: String) = BaseEncoding.base64.encode(MessageDigest.getInstance("SHA-256").digest(str.getBytes("UTF-8")))
  def sha256Base32(str: String) = BaseEncoding.base32.encode(MessageDigest.getInstance("SHA-256").digest(str.getBytes("UTF-8")))
  def sha1Base32(str: String) = BaseEncoding.base32.encode(MessageDigest.getInstance("SHA-1").digest(str.getBytes("UTF-8")))
  def murmur3(str: String): Long = MurmurHash3.MurmurHash3_x64_64(str.getBytes, 100416)
  def sha256ThenMurmur3(text: String): Long = MHash.murmur3(sha256Base64(text))
}
