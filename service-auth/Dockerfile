FROM eclipse-temurin:17-jdk AS builder

# ✅ builder 단계에서 Gradle 캐시 경로 변경
ENV GRADLE_USER_HOME=/home/gradle/.gradle

WORKDIR /app
COPY . .

# ✅ gradlew에 실행 권한 부여
RUN chmod +x ./gradlew

# ✅ 캐시 초기화 옵션 추가
RUN ./gradlew build -x test --no-daemon --refresh-dependencies

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]