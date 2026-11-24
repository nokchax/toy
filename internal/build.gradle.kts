dependencies {
    implementation(project(":crawl-core"))
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("tools.jackson.module:jackson-module-kotlin")

    // MCP 서버 (LLM 호출 기능은 쓰지 않는다). 2.0.x 가 Spring Boot 4.1 기준 — 2.1 은 Boot 4.2 를 끌고 온다
    implementation(platform("org.springframework.ai:spring-ai-bom:2.0.3"))
    implementation("org.springframework.ai:spring-ai-starter-mcp-server-webmvc")

    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
}
