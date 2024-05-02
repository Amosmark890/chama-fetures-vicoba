FROM maven:3.6.3-jdk-11 as build

WORKDIR /app

COPY pom.xml .
COPY src src

RUN mvn -T 1C package -DskipTests

FROM adoptopenjdk/openjdk11:jre-11.0.6_10-alpine

COPY --from=build /app/target/chama-kyc.jar /app/chama-kyc.jar

ENTRYPOINT ["java","-jar","/app/chama-kyc.jar"]