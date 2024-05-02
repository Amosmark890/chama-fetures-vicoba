FROM eclipse-temurin:17.0.7_7-jre-alpine

WORKDIR /app

COPY APIGateway-0.0.1-SNAPSHOT.jar /app/api-gateway.jar
COPY dcbvicoba.p12 /app/cert/dcbvicoba.p12

ENTRYPOINT ["java","-jar","/app/api-gateway.jar"]
