// 텔레그램 라이브러리는 이 모듈에만 둔다 → 다른 앱은 텔레그램으로 직접 보낼 수 없고 알림 큐를 거친다
dependencies {
    implementation(project(":crawl-core"))
    implementation("org.telegram:telegrambots-longpolling:10.3.0")
    implementation("org.telegram:telegrambots-client:10.3.0")
}
