package services.sms

import scala.concurrent._
import ExecutionContext.Implicits.global
import java.util.{List => JList, ArrayList}
import javax.net.ssl.SSLPeerUnverifiedException

import play.api.Logger
import com.twilio.sdk.TwilioRestClient
import com.twilio.sdk.TwilioRestException
import com.twilio.sdk.resource.factory.MessageFactory
import org.apache.http.NameValuePair
import org.apache.http.message.BasicNameValuePair
import org.apache.http.conn.ConnectTimeoutException
import com.coinport.coinex.api.model._
import services.SmsService

object TwilioSmsService extends SmsService {
  val log = Logger(this.getClass)

  val twilio = smsConfig.getConfig("sms.twilio")
  val twilioNumber = twilio.getString("twilioFromNumber")
  val accountSid = twilio.getString("accountSid")
  val authToken = twilio.getString("authToken")
  val retryTimes = twilio.getInt("retryTimes")

  val client = new TwilioRestClient(accountSid, authToken)
  val mainAccount = client.getAccount()
  val messageFactory = mainAccount.getMessageFactory

  override def sendSmsSingle(phoneNum: String, text: String): Future[ApiResult] =
    future {
      val messageParams: JList[NameValuePair] = new ArrayList[NameValuePair]()
      messageParams.add(new BasicNameValuePair("To", phoneNum))
      messageParams.add(new BasicNameValuePair("From", twilioNumber))
      messageParams.add(new BasicNameValuePair("Body", text))

      sendSmsWithRetry(messageParams, retryTimes)
    }

  private def sendSmsWithRetry(messageParams: JList[NameValuePair], retry: Int): ApiResult =
    try {
      val message = messageFactory.create(messageParams)
      log.info(s"sendSmsWithRetry, message: $message")
      successResult
    } catch {
      case e @ (_: TwilioRestException | _: SSLPeerUnverifiedException |
          _: ConnectTimeoutException) =>
        log.error(e.getMessage, e)
        if (retry > 0) {
          log.info(s"send sms failed. retrying, retry times remaining: $retry")
          sendSmsWithRetry(messageParams, retry - 1)
        } else
          ApiResult(false, -1, e.getMessage, None)
      case e2: Exception =>
        log.error(e2.getMessage, e2)
        ApiResult(false, -1, e2.getMessage, None)
    }

  override def sendSmsGroup(phoneNums: Set[String], text: String): Future[ApiResult] = future {
    failedResult
  }

  override def sendVerifySms(phoneNum: String, randCode: String): Future[ApiResult] = {
    val verifyMessage = createVerifyMessage(randCode)
    sendSmsSingle(phoneNum, verifyMessage)
  }

  private def createVerifyMessage(randCode: String) =
    s"Your verification code is $randCode [coinport]"

}
