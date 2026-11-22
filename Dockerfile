# NAS(amd64)에서 바로 빌드할 수 있도록 빌드까지 컨테이너 안에서 한다
FROM eclipse-temurin:25-jdk AS build
WORKDIR /src
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle gradle
RUN sed -i '/org.gradle.java.installations.paths/d' gradle.properties && ./gradlew --version -q
COPY common common
COPY crawler crawler
COPY bot bot
RUN ./gradlew :bot:bootJar --no-daemon -q

FROM eclipse-temurin:25-jre
ENV TZ=Asia/Seoul
WORKDIR /app
RUN useradd --system --uid 1001 toy
COPY --from=build /src/bot/build/libs/bot-*.jar app.jar
USER toy
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
