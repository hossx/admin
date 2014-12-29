package services.sms

import java.util.UUID
import java.util.Random
import java.util.concurrent.TimeUnit

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder

object VerifyCodeManager {
  val cache: Cache[String, String] = CacheBuilder.newBuilder()
    .maximumSize(100000)
    .expireAfterWrite(5, TimeUnit.MINUTES)
    .build()

  val rand = new Random()
  val randMax = 999999
  val randMin = 100000

  def generateVerifyCode: (String, String) = {
    val uuid = UUID.randomUUID().toString
    val verifyNum = rand.nextInt(randMax - randMin) + randMin
    val verifyCode = verifyNum.toString
    cache.put(uuid, verifyCode)
    (uuid, verifyCode)
  }

  def verify(uuid: String, verifyCode: String): Boolean = {
    val verifyCodeCached = cache.getIfPresent(uuid)
    if (verifyCodeCached == null) false
    else
      verifyCodeCached.equals(verifyCode)
  }
}
