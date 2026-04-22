FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /build

COPY pom.xml .
COPY api-gateway ./api-gateway
COPY auth-service ./auth-service
COPY wallet-service ./wallet-service
COPY shared ./shared
COPY payment-service ./payment-service
COPY ledger-service/pom.xml ./ledger-service/pom.xml
COPY notification-service/pom.xml ./notification-service/pom.xml

RUN mvn -pl api-gateway -am clean package -Dmaven.test.skip=true

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /build/api-gateway/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]