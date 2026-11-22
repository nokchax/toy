dependencies {
    api(project(":common"))
    implementation("org.jsoup:jsoup:1.24.1")

    testRuntimeOnly("com.h2database:h2")
}
