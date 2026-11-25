# Stage 1: Build (Maven + JDK 25)
FROM maven:3-eclipse-temurin-25 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn package -DskipTests -B

# Stage 2: Runtime (nur JRE 25)
FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /app/target/grade-compass-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
