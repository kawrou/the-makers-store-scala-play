package features

import org.scalatestplus.selenium._
import org.openqa.selenium.{By, WebDriver}
import org.openqa.selenium.chrome.ChromeDriver
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.Duration


class HomePageSpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll{

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
  "The homepage" should "display the correct title" in {
      driver.get("http://localhost:9000/")

      val heading = driver.findElement(By.tagName("h1")).getText
      val h2 = driver.findElement(By.tagName("h2")).getText
      heading shouldEqual "The Makers Store"
      h2 shouldEqual "Welcome to The Makers Store!"

    }
}
