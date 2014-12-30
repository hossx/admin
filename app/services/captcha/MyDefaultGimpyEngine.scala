package services.captcha

import java.lang.{Boolean => JBoolean}
import java.util.Locale
import java.awt.Color
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.awt.image.ImageFilter
import java.awt.image.BufferedImage
import java.io.Serializable

import com.coinport.coinex.api.model._

import com.octo.captcha.engine.image.gimpy.DefaultGimpyEngine
import com.octo.captcha.component.image.backgroundgenerator.BackgroundGenerator
import com.octo.captcha.component.image.backgroundgenerator.UniColorBackgroundGenerator
import com.octo.captcha.component.image.color.SingleColorGenerator
import com.octo.captcha.component.image.fontgenerator.FontGenerator
import com.octo.captcha.component.image.fontgenerator.RandomFontGenerator
import com.octo.captcha.component.image.textpaster.DecoratedRandomTextPaster
import com.octo.captcha.component.image.textpaster.TextPaster
import com.octo.captcha.component.image.textpaster.textdecorator.BaffleTextDecorator
import com.octo.captcha.component.image.textpaster.textdecorator.LineTextDecorator
import com.octo.captcha.component.image.textpaster.textdecorator.TextDecorator
import com.octo.captcha.component.image.wordtoimage.ComposedWordToImage
import com.octo.captcha.component.image.wordtoimage.WordToImage
import com.octo.captcha.component.word.wordgenerator.RandomWordGenerator
import com.octo.captcha.component.word.wordgenerator.WordGenerator
import com.octo.captcha.engine.image.ListImageCaptchaEngine
import com.octo.captcha.image.gimpy.GimpyFactory
import com.jhlabs.image.WaterFilter
import com.octo.captcha.component.image.deformation.ImageDeformation
import com.octo.captcha.component.image.deformation.ImageDeformationByFilters
import com.octo.captcha.component.image.wordtoimage.DeformedComposedWordToImage
import com.octo.captcha.component.word.FileDictionary
import com.octo.captcha.component.word.wordgenerator.ComposeDictionaryWordGenerator
import com.octo.captcha.image.ImageCaptcha
import com.octo.captcha.CaptchaQuestionHelper
import com.octo.captcha.CaptchaException

class MyDefaultGimpyEngine extends ListImageCaptchaEngine {
  private def getProperFonts: Array[Font] = {
    val properFontFamilies = Array[String]("Serif", "SansSerif", "Monospaced", "Tahoma", "Arial", "Helvetica", "Times", "Courier").map(_.toUpperCase)

    val e: GraphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment
    val allFonts: Array[Font] = e.getAllFonts

    allFonts.filter {
      font =>
      val family = font.getFamily.toUpperCase
      properFontFamilies.exists(family.contains(_))
    }
  }

  def buildInitialFactories() {
    val water: WaterFilter = new WaterFilter()
    water.setAmplitude(3d)
    water.setAntialias(true)
    water.setPhase(20d)
    water.setWavelength(70d)

    val backDef: ImageDeformation = new ImageDeformationByFilters(Array[ImageFilter]())
    val textDef: ImageDeformation = new ImageDeformationByFilters(Array[ImageFilter]())
    val postDef: ImageDeformation = new ImageDeformationByFilters(Array[ImageFilter](water))

    //word generator
    val dictionnaryWords: WordGenerator = new ComposeDictionaryWordGenerator(
      new FileDictionary("toddlist"))
    //wordtoimage components
    val randomPaster: TextPaster = new DecoratedRandomTextPaster(
      new Integer(4),
      new Integer(5),
      new SingleColorGenerator(Color.black),
      Array[TextDecorator](new BaffleTextDecorator(new Integer(2), Color.white))
      //Array[TextDecorator]()
    )

    val back: BackgroundGenerator = new UniColorBackgroundGenerator(
      new Integer(115), new Integer(50), Color.white)

    val shearedFont: FontGenerator = new RandomFontGenerator(new Integer(24),
      new Integer(28), getProperFonts)
    //word2image 1
    val word2image: WordToImage = new DeformedComposedWordToImage(
      shearedFont, back, randomPaster,
      backDef, textDef, postDef
    )

    addFactory(new MyGimpyFactory(dictionnaryWords, word2image))
  }

}

class MyGimpyFactory(generator: WordGenerator, word2image: WordToImage) extends GimpyFactory(generator, word2image) {
  override def getImageCaptcha(locale: Locale): ImageCaptcha = {
    val wordLength = getRandomLength()
    val word = getWordGenerator().getWord(wordLength, locale)
    try {
      val image = getWordToImage().getImage(word)
      new MyGimpy(CaptchaQuestionHelper.getQuestion(locale, GimpyFactory.BUNDLE_QUESTION_KEY), image, word)
    } catch {
      case e: Throwable => null
    }
  }
}

class MyGimpy(question: String, challenge: BufferedImage, response: String) extends ImageCaptcha(question, challenge) with Serializable {

  final def validateResponse(response: Object): JBoolean = {
    if (null != response && response.isInstanceOf[String]) validateResponse(response.asInstanceOf[String]) else false
  }

  private final def validateResponse(response: String): Boolean = {
    response.equals(this.response)
  }

  val getResponse = response
}

class CaptchaEngineEx extends DefaultGimpyEngine {
  private def getProperFonts: Array[Font] = {
    val properFontFamilies = Array[String]("Serif", "SansSerif", "Monospaced", "Tahoma", "Arial", "Helvetica", "Times", "Courier").map(_.toUpperCase)

    val e: GraphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment
    val allFonts: Array[Font] = e.getAllFonts

    allFonts.filter {
      font =>
      val family = font.getFamily.toUpperCase
      properFontFamilies.exists(family.contains(_))
    }
  }

  override def buildInitialFactories {
    //Set Captcha Word Length Limitation which should not over 6
    val minAcceptedWordLength = new Integer(4)
    val maxAcceptedWordLength = new Integer(5)

    //Set up Captcha Image Size: Height and Width
    val imageHeight = new Integer(45)
    val imageWidth = new Integer(110)

    //Set Captcha Font Size
    val minFontSize = new Integer(22)
    val maxFontSize = new Integer(24)

    val wordGenerator: WordGenerator = new RandomWordGenerator("abcdefghijklmnopqrstuvwxyz")

    // val bgGen: BackgroundGenerator = new GradientBackgroundGenerator(
    //   imageWidth, imageHeight, new Color(0xE0, 0xE8, 0xF0), new Color(0xE0, 0xFF, 0xFF))
    val bgGen: BackgroundGenerator = new UniColorBackgroundGenerator( imageWidth, imageHeight)


    //font is not helpful for security but it really increase difficultness for attacker
    val fontGenerator: FontGenerator = new RandomFontGenerator(minFontSize,
      maxFontSize, getProperFonts)

    // Note that our captcha color is Blue
    val scg: SingleColorGenerator = new SingleColorGenerator(Color.blue)

    //decorator is very useful pretend captcha attack. we use two line text
    //decorators.
    // LineTextDecorator lineDecorator = new LineTextDecorator(1, Color.blue);
    // LineTextDecorator line_decorator2 = new LineTextDecorator(1, Color.blue);
    // TextDecorator[] textdecorators = new TextDecorator[1];

    // textdecorators[0] = lineDecorator;
    // textdecorators[1] = line_decorator2;

    val textPaster: TextPaster = new DecoratedRandomTextPaster(
      minAcceptedWordLength, maxAcceptedWordLength, scg,
      Array[TextDecorator](new BaffleTextDecorator(new Integer(1), Color.white)))

    //ok, generate the WordToImage Object for logon service to use.
    val wordToImage: WordToImage = new ComposedWordToImage(fontGenerator, bgGen, textPaster)
    addFactory(new GimpyFactory(wordGenerator, wordToImage))
  }

}
