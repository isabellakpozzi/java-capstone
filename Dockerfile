# --- Build stage: compiles the app with a real JDK + Maven ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy just the wrapper + pom first so dependency downloads can be cached
# separately from source code changes (faster rebuilds on future deploys).
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw

COPY src src
RUN ./mvnw clean package -DskipTests

# --- Run stage: smaller image, JRE only, no build tools needed at runtime ---
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]