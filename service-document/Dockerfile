FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app
COPY . .
RUN ./gradlew build -x test

FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
COPY ./libs/hwp2hwpx-1.0.0.jar /app/libs/hwp2hwpx-1.0.0.jar
ENTRYPOINT ["java", "-jar", "app.jar"]