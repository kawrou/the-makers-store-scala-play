package features

import org.scalatestplus.selenium._
import org.openqa.selenium.{By, WebDriver}
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.support.ui.{ExpectedConditions, WebDriverWait}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.Duration


class LogInSpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver")
  val driver: WebDriver = new ChromeDriver()
  //  override def beforeAll(): Unit  ={
  //    System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver")
  //    val driver: WebDriver = new ChromeDriver()
  //    driver.manage().timeouts().implicitlyWait(Duration.ofMillis(100))
  //  }

  override def beforeEach() = {
    driver.get("http://localhost:9000/login")
  }

  override def afterAll(): Unit = {
    driver.quit()
  }

  "Log In page" should "display a Log In form" in {
    //    driver.get("http://localhost:9000/login")

    val heading = driver.findElement(By.tagName("h1")).getText
    val h2 = driver.findElement(By.tagName("h2")).getText
    val form = driver.findElement(By.id("log-in-form"))
    heading shouldEqual "Log In"
    h2 shouldEqual "Log in to your account"
    form should not be null
    form.isDisplayed shouldBe true
  }

  "Logging in" should "redirect to the items page" in {
    val wait = new WebDriverWait(driver, Duration.ofMillis(100))

    val usernameInput = driver.findElement(By.id("username"))
    val passwordInput = driver.findElement(By.id("password"))
    val submitButton = driver.findElement(By.id("login-button"))

    usernameInput.sendKeys("testuser")
    passwordInput.sendKeys("Password1")
    submitButton.click()

    wait.until(ExpectedConditions.urlToBe("http://localhost:9000/items")) // Adjust the URL as needed
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("h2")))

    val itemsPageH2 = driver.findElement(By.tagName("h2")).getText

    itemsPageH2 shouldEqual "Browse all our items!"
  }
}
