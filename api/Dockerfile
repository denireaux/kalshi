FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./
RUN chmod +x gradlew

COPY src ./src
RUN ./gradlew --no-daemon bootJar -x test

FROM gcr.io/distroless/java21-debian12:nonroot
WORKDIR /app
COPY --from=build /app/build/libs/app.jar /app/app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]
