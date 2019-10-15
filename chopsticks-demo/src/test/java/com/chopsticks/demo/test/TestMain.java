package com.chopsticks.demo.test;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.gargoylesoftware.css.parser.CSSErrorHandler;
import com.gargoylesoftware.css.parser.CSSException;
import com.gargoylesoftware.css.parser.CSSParseException;
import com.gargoylesoftware.htmlunit.AlertHandler;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.ScriptResult;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebClientOptions;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.machinepublishers.jbrowserdriver.JBrowserDriver;

public class TestMain {
	@Test
	public void testHtmlUnit() throws Throwable{
		final CountDownLatch wait = new CountDownLatch(1);
		final String finished = "MathJax Done";
		final WebClient webClient = new WebClient();
		webClient.setCssErrorHandler(new CSSErrorHandler() {
			@Override
			public void warning(CSSParseException exception) throws CSSException {
			}
			@Override
			public void fatalError(CSSParseException exception) throws CSSException {
			}
			@Override
			public void error(CSSParseException exception) throws CSSException {
			}
		});
		WebClientOptions options = webClient.getOptions();
		options.setCssEnabled(false);
		options.setThrowExceptionOnScriptError(false);
		options.setThrowExceptionOnFailingStatusCode(false);
		options.setDownloadImages(false);
		File htmlFile = new File(TestMain.class.getResource("/math_test.html").getFile());
		webClient.setAlertHandler(new AlertHandler() {
			@Override
			public void handleAlert(Page page, String message) {
				if(finished.equals(message)) {
					wait.countDown();
				}else {
					System.out.println(message);
				}
			}
		});
		HtmlPage page = webClient.getPage(htmlFile.toURI().toURL().toString());
		ScriptResult result = page.executeJavaScript("MathJax.Hub.Queue(function(){window.alert('MathJax Done');})");
		wait.await();
		page = (HtmlPage) result.getNewPage();
		Files.write(page.asXml(), new File(TestMain.class.getResource("/").getFile(), "result.html"), Charsets.UTF_8);
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
	@Test
	public void testR() throws Throwable {
		System.out.println("TMS_UPSTREAM_MODIFY_ORDER_ASYNC-1B0D7BD3E2F76776B6091A347DF8FC23".hashCode() % 512);
		System.out.println("MODIFY_MATERIAL-17345847749931A59229D6620D3DED23".hashCode() % 512);
		System.out.println("TMS_UPSTREAM_MODIFY_ORDER_ASYNC-C559DA2BA967EB820766939A658022C8".hashCode() % 512);
		System.out.println("TMS_UPSTREAM_ORDER_CANCEL_ASYNC-F4089A008A6262B0350BF9630729D681".hashCode() % 512);
		System.out.println("ORDER_SIGN_SERVICE-66D849126DDADB10874FE61E2F129894".hashCode() % 512);
		
	}
}
