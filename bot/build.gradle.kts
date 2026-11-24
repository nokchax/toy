plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":crawler"))
    implementation("org.telegram:telegrambots-longpolling:10.3.0")
    implementation("org.telegram:telegrambots-client:10.3.0")

    runtimeOnly("com.h2database:h2")
    testRuntimeOnly("com.h2database:h2")
}

tasks.jar {
    enabled = false
}

// 프로젝트 루트에서 실행해야 .env 와 ./data(H2) 를 루트 기준으로 찾는다
tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    workingDir = rootProject.projectDir
}
