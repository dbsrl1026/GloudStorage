# 1단계: 빌드용 스테이지
FROM gradle:7.6.3-jdk17 AS builder

WORKDIR /app

# 권한 문제 해결용 캐시 디렉토리 변경
ENV GRADLE_USER_HOME=/tmp/.gradle

COPY . .

RUN gradle build -x test --no-daemon

FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]