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
