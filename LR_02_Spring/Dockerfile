FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
RUN mvn -B -ntp clean verify

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN groupadd --system jewelry && useradd --system --gid jewelry jewelry
COPY --from=build --chown=jewelry:jewelry /build/target/jewelry-spring.jar /app/app.jar
USER jewelry
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
