package features

import org.scalatestplus.selenium._
import org.openqa.selenium.{By, WebDriver}
import org.openqa.selenium.chrome.ChromeDriver
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.Duration


class SignUpSpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll{

  System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver")
  val driver: WebDriver = new ChromeDriver()

  //  override def beforeAll(): Unit  ={
  //    System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver")
  //    val driver: WebDriver = new ChromeDriver()
  //    driver.manage().timeouts().implicitlyWait(Duration.ofMillis(100))
  //  }

  override def afterAll(): Unit = {
    driver.quit()
  }
  "Sign Up page" should "display a Sign Up form" in {
    driver.get("http://localhost:9000/signup")

    val heading = driver.findElement(By.tagName("h1")).getText
    val h2 = driver.findElement(By.tagName("h2")).getText
    val form = driver.findElement(By.id("signup-form"))
    heading shouldEqual "Sign Up"
    h2 shouldEqual "Sign up for an account"
    form should not be null
    form.isDisplayed shouldBe true
  }
}