FROM eclipse-temurin:25-jdk AS build

WORKDIR /workspace
COPY . .
RUN chmod +x mvnw && ./mvnw -pl worker -am package -DskipTests

FROM eclipse-temurin:25-jre

WORKDIR /app
COPY --from=build /workspace/worker/target/edifiq-worker-1.0.0.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
