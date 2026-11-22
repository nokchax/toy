# syntax=docker/dockerfile:1
# NAS 러너가 이 Dockerfile 로 직접 빌드한다 (deploy 방식 A). 빌드 단계는 최종 이미지에 남지 않는다
FROM eclipse-temurin:25-jdk AS build
WORKDIR /src
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle gradle
COPY common common
COPY crawler crawler
COPY bot bot
# Gradle 캐시를 BuildKit 캐시로 남겨서 다음 빌드부터 의존성을 다시 받지 않는다
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :bot:bootJar --no-daemon -q

FROM eclipse-temurin:25-jre
ENV TZ=Asia/Seoul
WORKDIR /app
RUN useradd --system --uid 1001 toy
COPY --from=build /src/bot/build/libs/bot-*.jar app.jar
USER toy
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
