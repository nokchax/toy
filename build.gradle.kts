import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    kotlin("jvm") version "2.4.21" apply false
    kotlin("plugin.spring") version "2.4.21" apply false
    kotlin("plugin.jpa") version "2.4.21" apply false
    id("org.springframework.boot") version "4.1.3" apply false
}

allprojects {
    group = "com.nokchax.toy"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }

    configure<KotlinJvmProjectExtension> {
        compilerOptions {
            freeCompilerArgs.add("-Xjsr305=strict")
        }
    }

    dependencies {
        // dependency-management 플러그인은 Kotlin 컴파일러 classpath 까지 BOM 버전으로 바꿔버려서 platform() 으로 적용한다
        "implementation"(platform(SpringBootPlugin.BOM_COORDINATES))
        "testImplementation"(platform(SpringBootPlugin.BOM_COORDINATES))
        "testRuntimeOnly"(platform(SpringBootPlugin.BOM_COORDINATES))
        "runtimeOnly"(platform(SpringBootPlugin.BOM_COORDINATES))

        "implementation"("org.jetbrains.kotlin:kotlin-reflect")
        "implementation"("io.github.oshai:kotlin-logging-jvm:8.0.7")

        "testImplementation"("org.springframework.boot:spring-boot-starter-test")
        "testImplementation"("org.jetbrains.kotlin:kotlin-test-junit5")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

/** 따로 실행 · 배포되는 앱 모듈. 배포 태그의 접미사와 Docker 이미지 이름도 이 이름을 쓴다. */
val apps = setOf("bot", "crawler", "internal", "external")

configure(subprojects.filter { it.name in apps }) {
    apply(plugin = "org.springframework.boot")

    dependencies {
        "runtimeOnly"("com.h2database:h2") // local 프로필
        "testRuntimeOnly"("com.h2database:h2")
    }

    // 실행용 bootJar 만 만든다
    tasks.named<Jar>("jar") {
        enabled = false
    }

    // 프로젝트 루트에서 실행해야 .env 와 ./data(H2) 를 루트 기준으로 찾는다
    tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
        workingDir = rootProject.projectDir
    }
}
