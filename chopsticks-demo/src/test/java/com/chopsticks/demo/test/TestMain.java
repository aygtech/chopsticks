package com.chopsticks.demo.test;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.machinepublishers.jbrowserdriver.JBrowserDriver;

public class TestMain {
	@Test
	public void testHtmlUnit() throws Throwable{
		final WebClient webClient = new WebClient();
		File htmlFile = new File("C:\\Users\\lzl\\Documents\\WXWork\\1688851555278454\\Cache\\File\\2019-07\\math_test.html");
		HtmlPage page = webClient.getPage(htmlFile.toURI().toURL().toString());
		System.out.println(page.asXml());
		webClient.close();
	}
	@Test
	public void testJBrowserDriver() throws Throwable{
		JBrowserDriver driver = new JBrowserDriver();
		File htmlFile = new File("C:\\Users\\lzl\\Documents\\WXWork\\1688851555278454\\Cache\\File\\2019-07\\math_test.html");
		driver.get(htmlFile.toURI().toURL().toString());
		System.out.println(driver.getPageSource());
		driver.quit();
	}
	@Test
	public void testPhantomJS() throws Throwable{
		if("Linux".equals(System.getProperty("os.name"))){
			System.setProperty("phantomjs.binary.path", TestMain.class.getResource("/phantomjs").getFile());
		}else{
			System.setProperty("phantomjs.binary.path", TestMain.class.getResource("/phantomjs.exe").getFile());
		}
		DesiredCapabilities dCaps = new DesiredCapabilities();
	    dCaps.setJavascriptEnabled(true);
	    dCaps.setCapability("takesScreenshot", false);
	    WebDriver driver = new PhantomJSDriver(dCaps);
	    driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
	    File htmlFile = new File("C:\\Users\\lzl\\Documents\\WXWork\\1688851555278454\\Cache\\File\\2019-07\\math_test.html");
		driver.get(htmlFile.toURI().toURL().toString());
		System.out.println(driver.getPageSource());
		driver.quit();
	}
}
