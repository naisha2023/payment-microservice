FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml ./pom.xml
COPY shared/pom.xml ./shared/pom.xml
COPY auth-service/pom.xml ./auth-service/pom.xml
COPY wallet-service/pom.xml ./wallet-service/pom.xml
COPY api-gateway/pom.xml ./api-gateway/pom.xml
COPY payment-service/pom.xml ./payment-service/pom.xml
COPY ledger-service/pom.xml ./ledger-service/pom.xml
COPY notification-service/pom.xml ./notification-service/pom.xml

RUN mvn -pl payment-service -am dependency:go-offline

COPY shared/src ./shared/src
COPY payment-service/src ./payment-service/src

RUN mvn -pl payment-service -am clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/payment-service/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java","-jar","app.jar"]