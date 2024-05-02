FROM adoptopenjdk/maven-openjdk11:latest
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:resolve-plugins dependency:resolve