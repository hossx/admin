package services

import java.util.UUID
import java.util.Locale
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

import com.octo.captcha.service.image.ImageCaptchaService
import com.octo.captcha.service.image.DefaultManageableImageCaptchaService
import com.octo.captcha.service.captchastore.FastHashMapCaptchaStore

import com.google.common.io.BaseEncoding

import com.coinport.coinex.api.model.Captcha
import captcha._

object CaptchaService {
  val store = new FastHashMapCaptchaStore()
  val captchaService: ImageCaptchaService =
    new DefaultManageableImageCaptchaService(
      store, new MyDefaultGimpyEngine(), 180, 100000, 75000)

  val cacheService = CacheService.getDefaultServiceImpl

  def getCaptcha = {
    val uuid = UUID.randomUUID().toString()
    val baos = new ByteArrayOutputStream
    ImageIO.write(captchaService.getImageChallengeForID(uuid, Locale.getDefault()), "jpg", baos)
    val imageBase64 = BaseEncoding.base64.encode(baos.toByteArray())
    val imageSrc = "data:image/jpeg;base64," + imageBase64
    val captchaObj = store.getCaptcha(uuid)
    //println(s"captchaObj class: ${captchaObj.getClass}")
    val myGimpy = captchaObj.asInstanceOf[MyGimpy]
    //println(s"myGimpy class: ${myGimpy.getResponse}")
    cacheService.putWithTimeout(uuid, myGimpy.getResponse, 30 * 60)

    Captcha(imageSrc, uuid)
  }

}
