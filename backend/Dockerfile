FROM maven:3-eclipse-temurin-25 AS build
WORKDIR /app
COPY . .
RUN ./mvnw -q -DskipTests package && cp target/*.jar app.jar

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-Duser.timezone=Europe/Istanbul", "-jar", "app.jar"]
