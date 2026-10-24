FROM maven:3.9.12-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system sigesu && useradd --system --gid sigesu sigesu
COPY --from=build --chown=sigesu:sigesu /app/target/sigesu-backend-1.0.0-SNAPSHOT.jar app.jar
USER sigesu
EXPOSE 8080
ENTRYPOINT ["java","-XX:MaxRAMPercentage=70","-jar","app.jar"]
