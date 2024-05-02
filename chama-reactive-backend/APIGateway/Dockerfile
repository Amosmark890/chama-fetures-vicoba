FROM api-gateway-builder:latest as builder
WORKDIR /app
COPY . .
RUN mvn package -Dmaven.test.skip=true

FROM adoptopenjdk/openjdk11:alpine-jre
WORKDIR /app
COPY  --from=builder /app/target/*.jar /app/app.jar
ENTRYPOINT ["java","-jar","app.jar"]