FROM eclipse-temurin:17-jdk AS builder

# 캐시 디렉토리 변경
ENV GRADLE_USER_HOME=/tmp/.gradle

WORKDIR /app
COPY . .
RUN ./gradlew build -x test --no-daemon

FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
COPY ./libs/hwp2hwpx-1.0.0.jar /app/libs/hwp2hwpx-1.0.0.jar
ENTRYPOINT ["java", "-jar", "app.jar"]