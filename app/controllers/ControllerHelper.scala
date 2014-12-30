package controllers

import com.typesafe.config.ConfigFactory
import controllers.GoogleAuth.GoogleAuthenticator
import java.io.FileInputStream
import java.io.File
import java.util.Properties
import play.api.Logger
import play.api.Play
import play.api.Play.current
import play.api.i18n.Lang
import play.api.mvc._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConversions._
import scala.concurrent.Future
import services.CacheService
import com.typesafe.config.Config

import com.coinport.coinex.api.model._
import com.coinport.coinex.data.ErrorCode

case class Pager(skip: Int = 0, limit: Int = 10, page: Int)

trait AccessLogging {
  val accessLogger = Logger("access")

  object AccessLoggingAction extends ActionBuilder[Request] {
    def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {
      accessLogger.info(s"method=${request.method} uri=${request.uri} remote-address=${request.remoteAddress}")
      block(request)
    }
  }
}

trait Validator {
  def result: ApiResult
  def validate: Either[ApiResult, Boolean]
  def logger: Logger = Logger("validator")
}

abstract class GeneralValidator[T](params: T*) extends Validator {
  def isValid(t: T): Boolean
  def validate = validate(params)

  private def validate(params: Seq[T]): Either[ApiResult, Boolean] =
    if (params.isEmpty) Right(true)
    else {
      if (!isValid(params.head))
        Left(result)
      else
        validate(params.tail)
    }
}

class PasswordValidator(email: String, password: String) extends Validator {
  val result = ApiResult(false, ErrorCode.PasswordNotMatch.value, "password not match", None)
  def validate = {
    val preDefinedUsers = loadUserFromConfig
    preDefinedUsers.find(_.email.equals(email)) match {
      case Some(user) => if(user.password.equals(password)) Right(true) else Left(result)
      case None => Left(result)
    }
  }

  private def loadUserFromConfig(): Seq[models.User] = {
    val userConfigFile = "/var/coinport/private/users.conf"
    val config = ConfigFactory.parseFile(new File(userConfigFile))
    val users = config.getConfigList("users") map {
      u: Config =>
      models.User(u.getString("email"), u.getString("name"), u.getString("password"))
    }
    users.toSeq
  }
}

class CachedValueValidator(error: ErrorCode, check: Boolean, uuid: String, value: String) extends Validator {
  val cacheService = CacheService.getDefaultServiceImpl
  val result = ApiResult(false, error.value, error.toString)

  def validate = {
    if (!check) Right(true)
    else {
      if (uuid == null || uuid.trim.isEmpty || value == null || value.trim.isEmpty) Left(result) else {
        val cachedValue = cacheService.get(uuid)
        logger.info(s" validate cached value. uuid: $uuid, cachedValue: $cachedValue")
        if (cachedValue != null && cachedValue.equals(value)) {
          //cacheService.pop(uuid)
          Right(true)
        } else Left(result)
      }
    }
  }
}

class GoogleAuthValidator(error: ErrorCode, secret: String, code: String) extends Validator {
  val result = ApiResult(false, error.value, error.toString)

  val codeInt = try{code.toInt} catch {case e: Exception => 0}
  val googleAuthenticator = new GoogleAuthenticator()

  def validate = {
    logger.info(s"google auth validator: secret: $secret, code: $code")
    if (secret == null || secret.trim.isEmpty) Right(true)
    else if(codeInt == 0) Left(result)
    else {
      if (googleAuthenticator.authorize(secret.toString, codeInt)) {
        logger.info(s"google authentication success")
        Right(true)
      } else {
        logger.info(s"google authentication failed")
        Left(result)
      }
    }
  }
}

class LoginFailedFrequencyValidator(email: String, ip: String) extends Validator {
  import LoginFailedFrequencyValidator._
  val result = ApiResult(false, ErrorCode.LoginFailedAndLocked.value, "", None)

  def validate = {
    val count = getLoginFailedCount(email, ip)
    if (count >= 4){
      val unlockMinutes = getUnLockMinutes(email, ip)
      if (unlockMinutes < 120) {
        val res = ApiResult(false, ErrorCode.LoginFailedAndLocked.value, s"login failed more than 5 times, please re-login after ${120 - unlockMinutes} minutes", Some(120 - unlockMinutes))
        Left(res)
      } else {
        cleanLoginFailedRecord(email, ip)
        Right(true)
      }
    }
    else {
      Right(true)
    }
  }
}

object LoginFailedFrequencyValidator {
  val cacheService = CacheService.getDefaultServiceImpl

  private def generateCountKey(email: String, ip: String): String =
    email.trim + ip.trim + "LOGINFAILED"

  private def generateTsKey(email: String, ip: String): String =
    email.trim + ip.trim + "LOGINFAILEDLASTTIME"

  def getLoginFailedCount(email: String, ip: String): Int = {
    val key = generateCountKey(email, ip)
    val value = cacheService.get(key)
    if ( value != null) value.toInt else 0
  }

  def getUnLockMinutes(email: String, ip: String): Int = {
    val key = generateTsKey(email, ip)
    val value = cacheService.get(key)
    if (value != null) {
      ((System.currentTimeMillis - value.toLong) / (1000 * 60)).toInt
    }
    else {
      cacheService.put(key, System.currentTimeMillis.toString)
      0
    }
  }

  def putLoginFailedRecord(email: String, ip: String) {
    val countKey = generateCountKey(email, ip)
    val tsKey = generateTsKey(email, ip)
    val countVal = cacheService.get(countKey)
    if (countVal != null) {
      cacheService.put(countKey, (countVal.toInt + 1).toString)
    } else cacheService.put(countKey, "1")
    cacheService.put(tsKey, System.currentTimeMillis.toString)
  }

  def cleanLoginFailedRecord(email: String, ip: String) {
    val countKey = generateCountKey(email, ip)
    val tsKey = generateTsKey(email, ip)
    cacheService.put(countKey, "0")
  }
}

object ControllerHelper {
  val emptyParamError = ApiResult(false, ErrorCode.ParamEmpty.value, "param can not emppty", None)
  val emailFormatError = ApiResult(false, ErrorCode.InvalidEmailFormat.value, "email format error", None)
  val passwordFormatError = ApiResult(false, ErrorCode.InvalidPasswordFormat.value, "password format error", None)
  val inviteCodeError = ApiResult(false, ErrorCode.EmailNotBindWithInviteCode.value, "invalid invite code", None)
  val phoneNumberFormatError = ApiResult(false, ErrorCode.InvalidPhoneNumberFormat.value, "invalid mobile phone number", None)

  val supportLangs = ConfigFactory.load("application.conf").getString("application.langs").split(",").toList

  class StringNonemptyValidator(stringParams: String*) extends GeneralValidator[String](stringParams: _*) {
    val result = emptyParamError
    def isValid(param: String) = param != null && param.trim.length > 0
  }

  class EmailFormatValidator(emails: String*) extends GeneralValidator[String](emails: _*) {
    val result = emailFormatError
    val emailRegex = """^[-0-9a-zA-Z.+_]+@[-0-9a-zA-Z.+_]+\.[a-zA-Z]{2,4}$"""
    def isValid(param: String) = param.matches(emailRegex)
  }

  class PasswordFormetValidator(passwords: String*) extends GeneralValidator[String](passwords: _*) {
    val result = passwordFormatError
    def isValid(param: String) = param.trim.length > 6
  }

  class PhoneNumberValidator(phoneNum: String*) extends GeneralValidator[String](phoneNum: _*) {
    val result = phoneNumberFormatError
    def isValid(param: String) = {
      val res1 = param != null && param.trim.length > 9
      val res2 = if (param.startsWith("+86")) param.substring(3).trim.length == 11 else true
      res1 && res2
    }
  }

  def popCachedValue(uuids: String*): Unit = {
    val cacheService = CacheService.getDefaultServiceImpl
    uuids.foreach { uuid => cacheService.pop(uuid) }
  }

  // class EmailWithInviteCodeValidator(emails: String*) extends GeneralValidator[String](emails: _*) {
  //   val result = inviteCodeError
  //   def isValid(param: String): Boolean = {
  //     val props = new Properties()
  //     var input: FileInputStream = null
  //     try {
  //       input = new FileInputStream(UserController.usedInviteCodeFile)
  //       props.load(input)
  //       logger.info(s"email: $param, all emails: ${props.values}")
  //       props.values.contains(param)
  //     } catch {
  //       case e: Exception =>
  //         logger.error(e.getMessage, e)
  //         false
  //     } finally {
  //       input.close()
  //     }
  //   }
  // }

  def validateParamsAndThen(validators: Validator*)(f: => Future[ApiResult]): Future[ApiResult] =
    if (validators.isEmpty)
      f
    else {
      validators.head.validate match {
        case Left(r) => Future(r)
        case Right(b) => validateParamsAndThen(validators.tail: _*)(f)
      }
    }

  def getParam(queryString: Map[String, Seq[String]], param: String): Option[String] = {
    queryString.get(param).map(_(0))
  }

  def getParam(queryString: Map[String, Seq[String]], param: String, default: String): String = {
    queryString.get(param) match {
      case Some(seq) =>
        if (seq.isEmpty) default else seq(0)
      case None =>
        default
    }
  }

  def parsePagingParam()(implicit request: Request[_]): Pager = {
    val query = request.queryString
    val limit = getParam(query, "limit", "10").toInt min 200
    val page = getParam(query, "page", "1").toInt max 0
    val skip = (page - 1) * limit
    Pager(skip = skip, limit = limit, page = page)
  }

  def langFromRequestCookie(request: Request[_]): Lang = {
    val lang: String = request.cookies.get(Play.langCookieName) match {
      case Some(langCookie) => langCookie.value
      case None => if(request.acceptLanguages.size > 0) request.acceptLanguages(0).code else "zh-CN"
    }
    Lang(if (supportLangs.contains(lang)) lang else "zh-CN")
  }

  def stringNumberFormat(numberStr: String, digit: Int) = {
    if (numberStr == null) ""
    else {
      val dotPos = numberStr.indexOf('.')
      if (dotPos >= 0) numberStr.substring(0, dotPos + 2) else numberStr
    }
  }
  def strToLong(str: String): Option[Long] =
    try {
      Some(str.toLong)
    } catch {
      case _: Throwable => None
    }
}
