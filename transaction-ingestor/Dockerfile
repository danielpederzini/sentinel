FROM eclipse-temurin:26-jdk AS builder
WORKDIR /app

ENV GRADLE_OPTS="-Dorg.gradle.wrapper.networkTimeout=120000 -Dorg.gradle.internal.http.connectionTimeout=120000 -Dorg.gradle.internal.http.socketTimeout=120000"

COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN chmod +x gradlew

COPY src ./src
RUN /bin/bash -lc 'for attempt in 1 2 3; do ./gradlew --no-daemon bootJar -x test && exit 0; echo "Gradle build failed (attempt ${attempt}/3), retrying..."; sleep 5; done; exit 1'

FROM eclipse-temurin:26-jre
WORKDIR /app

COPY --from=builder /app/build/libs/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
