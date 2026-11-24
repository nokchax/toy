// 크롤 엔진 · 크롤 서비스 라이브러리. bot · crawler · internal 이 함께 쓴다
dependencies {
    api(project(":common"))
    implementation("org.jsoup:jsoup:1.24.1")
    // 원격 브라우저(compose 의 browser 서비스, selenium/standalone-chromium 와 같은 버전)에 붙는다
    implementation("org.seleniumhq.selenium:selenium-remote-driver:4.52.0")
    implementation("org.seleniumhq.selenium:selenium-chrome-driver:4.52.0")
    implementation("org.seleniumhq.selenium:selenium-support:4.52.0")

    testRuntimeOnly("com.h2database:h2")
}
