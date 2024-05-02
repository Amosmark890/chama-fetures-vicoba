#FROM eclipse-temurin:17.0.7_7-jre-alpine
#
#WORKDIR /app
#
#COPY Chama-Poll-0.0.1-SNAPSHOT.jar /app/chama-polls.jar
#
#ENTRYPOINT ["java","-jar","/app/chama-polls.jar"]

FROM adoptopenjdk/openjdk11:jre-11.0.6_10-alpine

COPY Chama-Poll-0.0.1-SNAPSHOT.jar /app/Chama-Poll-0.0.1-SNAPSHOT.jar

ENTRYPOINT ["java","-jar","/app/Chama-Poll-0.0.1-SNAPSHOT.jar"]

