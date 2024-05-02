FROM maven:3.6.3-jdk-11 as build

WORKDIR /app

COPY pom.xml .
COPY src src

RUN mvn -T 1C package -DskipTests

FROM adoptopenjdk/openjdk11:jre-11.0.6_10-alpine

COPY --from=build /app/target/Chama-Poll-0.0.1-SNAPSHOT.jar /app/Chama-Poll-0.0.1-SNAPSHOT.jar

ENTRYPOINT ["java","-jar","/app/Chama-Poll-0.0.1-SNAPSHOT.jar"]