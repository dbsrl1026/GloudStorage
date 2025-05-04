# 1단계: 빌드용 스테이지
FROM gradle:7.6.3-jdk17 AS builder
WORKDIR /app
COPY . .
RUN gradle build -x test

# 2단계: 실행 스테이지
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
COPY ./config /app/config
ENTRYPOINT ["java", "-jar", "app.jar"]