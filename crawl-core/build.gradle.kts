// 크롤 엔진 · 크롤 서비스 라이브러리. bot · crawler · internal 이 함께 쓴다
dependencies {
    api(project(":common"))
    implementation("org.jsoup:jsoup:1.24.1")

    testRuntimeOnly("com.h2database:h2")
}
