FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B -ntp clean verify

FROM payara/server-full:6.2025.10-jdk21
COPY --from=build --chown=payara:payara /build/target/jdbc/postgresql.jar /opt/payara/appserver/glassfish/domains/domain1/lib/postgresql.jar
COPY --from=build --chown=payara:payara /build/target/jewelry-shop.war /opt/payara/deployments/jewelry-shop.war
