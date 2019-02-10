package com.datengaertnerei.webdriver;

import io.github.bonigarcia.wdm.WebDriverManager;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.By;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * Wrapper class for Selenium WebDriver provides convenient access. - encapsulates TakesScreenshot -
 * uses local or remote driver - configurable
 */
public class UniversalWebDriver implements WebDriver, TakesScreenshot {

  private static final String DEFAULT_WEBDRIVER_PROPERTIES = "UniversalWebDriver.properties";
  private static final String DEFAULT_WEBDRIVER_CONFIG = "com.datengaertnerei.webdriver.config";
  private static final String WEBDRIVER_FLAVOR = "UniversalWebDriver.Flavor";
  private static final String CFG_WEBDRIVER_REMOTE_OPTIONS = "webdriver.remote.options.";
  private static final String CFG_WEBDRIVER_REMOTE_URL = "webdriver.remote.url";
  private static final String CFG_WEBDRIVER_LOCAL_CLASS = "webdriver.local.class.";
  private static final String CFG_TYPE_REMOTE = "remote";
  private static final String CFG_TYPE_LOCAL = "local";
  private static final String CFG_WEBDRIVER_TYPE = "webdriver.type";
  private static final String CFG_WEBDRIVER_FLAVORS = "webdriver.flavors";

  private static Log log = LogFactory.getLog(UniversalWebDriver.class);

  private WebDriver internalDriver;
  private TakesScreenshot ts;

  /** Default ctor uses system property for webdriver flavor. */
  public UniversalWebDriver() {
    this(System.getProperty(WEBDRIVER_FLAVOR));
  }

  /**
   * Constructor takes webdriver flavor as parameter.
   *
   * @param flavor one of the flavors from the config file
   */
  public UniversalWebDriver(String flavor) {
    // create the WebDriver
    createWebDriver(getConfiguration(), flavor.toLowerCase());
    // and cast to TakesScreenShot
    ts = (TakesScreenshot) internalDriver;
  }

  private void createWebDriver(Configuration config, String flavor) {
    if (null == config) {
      // no config present, use Chrome as fallback
      log.warn("No UniversalWebDriver configuration found.");
      createFallbackDriver();
      return;
    }

    // Fetch webdriver flavors from config
    List<String> flavors = Arrays.asList(config.getString(CFG_WEBDRIVER_FLAVORS).split(","));
    if (null == flavor) {
      flavor = flavors.get(0);
    } else if (!flavors.contains(flavor)) {
      createFallbackDriver();
      return;
    }

    // local or remote?
    if (config.getString(CFG_WEBDRIVER_TYPE, CFG_TYPE_LOCAL).equals(CFG_TYPE_REMOTE)) {
      internalDriver = createRemoteDriver(config, flavor);
    } else {
      // setup local webdriver
      String driverClazz = config.getString(CFG_WEBDRIVER_LOCAL_CLASS + flavor);
      try {
        // use the configured class
        Class<?> driverClass = Class.forName(driverClazz);
        // let WebDriverManager take care of the necessary webdriver binary
        WebDriverManager.getInstance(driverClass).setup();
        // create new webdriver instance
        internalDriver = (WebDriver) driverClass.getConstructor().newInstance();
      } catch (InstantiationException
          | IllegalAccessException
          | IllegalArgumentException
          | InvocationTargetException
          | NoSuchMethodException
          | SecurityException
          | ClassNotFoundException e) {
        log.error("could not instantiate driver class", e);
      }
    }

    if (null == internalDriver) {
      createFallbackDriver();
    }
  }

  private void createFallbackDriver() {
    // Fallback, use Chrome
    log.warn("config error, using fallback driver");
    WebDriverManager.chromedriver().setup();
    internalDriver = new ChromeDriver();
  }

  private WebDriver createRemoteDriver(Configuration config, String flavor) {
    URL webdriverRemoteUrl = null;
    Capabilities options = null;
    try {
      // compile RemoteWebDriver parameters from config	
      webdriverRemoteUrl = new URL(config.getString(CFG_WEBDRIVER_REMOTE_URL));
      String optionsClazz = config.getString(CFG_WEBDRIVER_REMOTE_OPTIONS + flavor);
      options = (Capabilities) Class.forName(optionsClazz).getConstructor().newInstance();
    } catch (MalformedURLException
        | InstantiationException
        | IllegalAccessException
        | IllegalArgumentException
        | InvocationTargetException
        | NoSuchMethodException
        | SecurityException
        | ClassNotFoundException e) {
      log.error("could not instantiate options class for remote driver", e);
    }
    return new RemoteWebDriver(webdriverRemoteUrl, options);
  }

  private Configuration getConfiguration() {
    Configuration config = null;
    Configurations configs = new Configurations();
    String configFile = System.getProperty(DEFAULT_WEBDRIVER_CONFIG);
    // access configuration properties
    try {
      if (null != configFile && configFile.length() > 0) {
        config = configs.properties(new File(configFile));
      } else {
        URL url = getClass().getResource(DEFAULT_WEBDRIVER_PROPERTIES);
        config = configs.properties(url);
      }

    } catch (ConfigurationException e) {
      log.error("could not read config", e);
    }
    return config;
  }

  @Override
  public <X> X getScreenshotAs(OutputType<X> target) {
    return ts.getScreenshotAs(target);
  }

  @Override
  public void get(String url) {
    internalDriver.get(url);
  }

  @Override
  public String getCurrentUrl() {
    return internalDriver.getCurrentUrl();
  }

  @Override
  public String getTitle() {
    return internalDriver.getTitle();
  }

  @Override
  public List<WebElement> findElements(By by) {
    return internalDriver.findElements(by);
  }

  @Override
  public WebElement findElement(By by) {
    return internalDriver.findElement(by);
  }

  @Override
  public String getPageSource() {
    return internalDriver.getPageSource();
  }

  @Override
  public void close() {
    internalDriver.close();
  }

  @Override
  public void quit() {
    internalDriver.quit();
  }

  @Override
  public Set<String> getWindowHandles() {
    return internalDriver.getWindowHandles();
  }

  @Override
  public String getWindowHandle() {
    return internalDriver.getWindowHandle();
  }

  @Override
  public TargetLocator switchTo() {
    return internalDriver.switchTo();
  }

  @Override
  public Navigation navigate() {
    return internalDriver.navigate();
  }

  @Override
  public Options manage() {
    return internalDriver.manage();
  }
}
