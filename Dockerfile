# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN --mount=type=secret,id=github_token \
    mkdir -p /root/.m2 && \
    printf '<settings><servers><server><id>github-micro-aws-common</id><username>xTiz02</username><password>%s</password></server></servers></settings>' "$(cat /run/secrets/github_token)" > /root/.m2/settings.xml && \
    mvn -B clean package -DskipTests && \
    rm -f /root/.m2/settings.xml

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
