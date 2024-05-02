FROM eclipse-temurin:17.0.7_7-jre-alpine

WORKDIR /app

COPY chamaAuthServer-0.0.1-SNAPSHOT.jar /app/auth-server.jar

ENTRYPOINT ["java","-jar","/app/auth-server.jar"]
