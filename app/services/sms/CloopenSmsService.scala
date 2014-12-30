package services.sms

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

import java.util.Date
import java.io.ByteArrayInputStream
import java.text.SimpleDateFormat
import java.security.MessageDigest
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.Principal
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.security.SecureRandom
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

import org.apache.http.conn.scheme.Scheme
import org.apache.http.conn.ssl.SSLSocketFactory
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.BasicHttpEntity
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.message.AbstractHttpMessage
import org.apache.http.util.EntityUtils

import com.typesafe.config.ConfigFactory
import com.google.common.io.BaseEncoding
import com.github.tototoshi.play2.json4s.native.Json4s
import com.coinport.coinex.api.model._
import services._

// impl SMSService with service of www.yuntongxun.com(www.cloopen.com).
object CloopenSmsService extends SmsService {
  val UTF8 = "utf-8"

  val serverBaseUrl = "https://sandboxapp.cloopen.com:8883/2013-12-26"
  val cloopen = smsConfig.getConfig("sms.cloopen")
  val rootAccountSid = cloopen.getString("rootAccountSid")
  val authToken = cloopen.getString("authToken")
  val validateSmsTemplateId = cloopen.getString("validateSmsTemplateId")
  val cloopenHost = cloopen.getString("host")
  val cloopenPort: Int = cloopen.getInt("port")
  val appId: String = cloopen.getString("appId")

  val tm: X509TrustManager = new X509TrustManager {
    /**
      * 验证客户端证书
      */
    def checkClientTrusted(chain: Array[X509Certificate], authType: String): Unit = {
      //这里跳过客户端证书  验证
    }

    /**
      * 验证服务端证书
      * @param chain 证书链
      * @param authType 使用的密钥交换算法，当使用来自服务器的密钥时authType为RSA
      */
    def  checkServerTrusted(chain: Array[X509Certificate], authType: String): Unit = {
      if (chain == null || chain.length == 0)
        throw new IllegalArgumentException("null or zero-length certificate chain")
      if (authType == null || authType.length() == 0)
        throw new IllegalArgumentException("null or zero-length authentication type")

      if (chain.exists(_.getSubjectX500Principal != null)){
        // check passed. do nothing.
      } else {
        throw new CertificateException("服务端证书验证失败！")
      }
    }

    /**
      * 返回CA发行的证书
      */
    override def getAcceptedIssuers(): Array[X509Certificate] = {
      Array[X509Certificate]()
    }
  }

  private def registerSSL(hostName: String, protocol: String, port: Int, scheme: String): DefaultHttpClient = {
    val httpClient = new DefaultHttpClient()
    val ctx: SSLContext = SSLContext.getInstance(protocol)
    ctx.init(null, Array[TrustManager](tm), new SecureRandom())
    val socketFactory: SSLSocketFactory = new SSLSocketFactory(ctx,SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
    val sch: Scheme = new Scheme(scheme, port, socketFactory)
        //注册SSL连接
    httpClient.getConnectionManager().getSchemeRegistry().register(sch)
    httpClient
  }

  private def md5Digest(src: String): String = {
    val md: MessageDigest = MessageDigest.getInstance("MD5")
    val digestBytes = md.digest(src.getBytes(UTF8))
    digestBytes.map {
      b =>
      val s = Integer.toHexString(b & 0xFFE).toUpperCase
      if (s.length ==1) "0" + s else s
    }.mkString
  }

  private def base64Encode(src: String) = {
    val bytes = src.getBytes(UTF8)
    BaseEncoding.base64.encode(bytes)
  }

  private def parseResponse(responseStr: String): ApiResult = {
    println(s"responseStr: $responseStr")
    ApiResult(false, -1, "not implemented.", None)
  }

  private val sdf: SimpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss")
  override def sendSmsSingle(phoneNum: String, text: String): Future[ApiResult] = future {
    val subAccountSid: String = rootAccountSid // use root account directly.
    val httpClient = registerSSL(cloopenHost, "TLS", cloopenPort, "https")
    val timestamp = sdf.format(new Date())
    val sig = rootAccountSid + authToken + timestamp
    val signature = md5Digest(sig)

    val url = serverBaseUrl + "/Accounts/" + "/SubAccounts?sig=" + signature
    val httpPost = new HttpPost(url)
    httpPost.setHeader("Accept", "application/json")
    httpPost.setHeader("Content-Type", "application/json;charset=utf-8")
    val auth = base64Encode(subAccountSid + ":" + timestamp)
    httpPost.setHeader("Authorization", auth) // base64(主账户Id + 冒号 + 时间戳)

    val msgType = "0"
    val sendSmsRequest = SendSmsRequest(phoneNum, text, msgType, appId, subAccountSid)
    val bodyData = sendSmsRequest.toJson

    val requestBody: BasicHttpEntity = new BasicHttpEntity()
    requestBody.setContent(new ByteArrayInputStream(bodyData.toString.getBytes(UTF8)))
    requestBody.setContentLength(bodyData.toString.getBytes(UTF8).length)
    httpPost.setEntity(requestBody)

    val response: HttpResponse = httpClient.execute(httpPost)
    val responseEntity: HttpEntity = response.getEntity()
    val responseStr: String = EntityUtils.toString(responseEntity, UTF8)
    EntityUtils.consume(responseEntity)
    parseResponse(responseStr)
  }

  override def sendSmsGroup(phoneNums: Set[String], text: String): Future[ApiResult] = {
    future { ApiResult(false, -1, "not implemented 2.", None) }
  }
  override def sendVerifySms(phoneNum: String, randCode: String): Future[ApiResult] = {
    future { ApiResult(false, -1, "not implemented 3.", None) }
  }

}

private case class SendSmsRequest(to: String, body: String, msgType: String, appId: String, subAccountSid: String)

private case class SendTemplateSmsRequest(to: String, appId: String, templateId: String, datas: Seq[String])
