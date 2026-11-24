package com.nokchax.toy.crawl.fetch

import com.nokchax.toy.common.domain.crawl.SelectorType
import com.nokchax.toy.crawl.config.CrawlProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.openqa.selenium.By
import org.openqa.selenium.InvalidSelectorException
import org.openqa.selenium.TimeoutException
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebDriverException
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.remote.http.ClientConfig
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.springframework.stereotype.Component
import java.net.URI

/**
 * 계속 떠 있는 원격 브라우저(Selenium, compose 의 browser 서비스)에 붙어서 JS 를 실행한 뒤의 HTML 을 받는다.
 * 브라우저 프로세스는 컨테이너가 유지하고, 요청마다 세션(탭)만 열고 닫는다.
 */
@Component
class BrowserFetcher(
    private val properties: CrawlProperties,
) {
    private val log = KotlinLogging.logger {}

    fun fetch(url: String, waitFor: WaitFor?): FetchedPage {
        val driver = connect()
        try {
            driver.get(url)
            waitFor?.let { waitUntilPresent(driver, it) }
            return FetchedPage(driver.currentUrl ?: url, driver.pageSource.orEmpty())
        } catch (e: TimeoutException) {
            throw FetchException("브라우저 페이지 로딩 시간 초과 (${properties.browser.pageLoadTimeout.seconds}초)", e)
        } catch (e: WebDriverException) {
            throw FetchException("브라우저 오류: ${e.rawMessage?.lineSequence()?.firstOrNull() ?: e.javaClass.simpleName}", e)
        } finally {
            runCatching { driver.quit() }.onFailure { log.warn(it) { "browser session quit failed" } }
        }
    }

    private fun connect(): WebDriver {
        val address = properties.browser.url?.takeIf { it.isNotBlank() }
            ?: throw FetchException("브라우저 주소(toy.crawl.browser.url)가 설정되지 않음")
        val options = ChromeOptions()
            .addArguments("--headless=new", "--window-size=1366,2000")
            .setPageLoadTimeout(properties.browser.pageLoadTimeout)
        val config = ClientConfig.defaultConfig()
            .baseUri(URI(address))
            .readTimeout(properties.browser.readTimeout)

        return try {
            RemoteWebDriver.builder().oneOf(options).config(config).build()
        } catch (e: WebDriverException) {
            throw FetchException("브라우저에 연결하지 못함: ${e.rawMessage?.lineSequence()?.firstOrNull() ?: e.javaClass.simpleName}", e)
        }
    }

    /** 안 나타나도 지금 HTML 로 진행한다. 항목이 없으면 실행 쪽에서 실패로 기록한다. */
    private fun waitUntilPresent(driver: WebDriver, waitFor: WaitFor) {
        val by = when (waitFor.selectorType) {
            SelectorType.CSS -> By.cssSelector(waitFor.selector)
            SelectorType.XPATH -> By.xpath(waitFor.selector)
        }
        try {
            WebDriverWait(driver, properties.browser.waitTimeout).until(ExpectedConditions.presenceOfElementLocated(by))
        } catch (_: TimeoutException) {
            log.debug { "wait timeout: ${waitFor.selector}" }
        } catch (_: InvalidSelectorException) {
            // Jsoup 전용 문법(:contains 등)은 브라우저가 모른다. 기다리지 않고 진행한다
            log.debug { "browser cannot use selector: ${waitFor.selector}" }
        }
    }
}
