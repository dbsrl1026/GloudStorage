FROM eclipse-temurin:17-jdk AS builder

# Gradle 캐시 경로를 임시 디렉토리로 설정하여 권한 문제 회피
ENV GRADLE_USER_HOME=/tmp/.gradle

WORKDIR /app
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew build -x test --no-daemon

FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]