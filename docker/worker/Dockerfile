FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /app

COPY . .

RUN ./mvnw clean install -N
RUN ./mvnw -pl shared install
RUN ./mvnw -f worker/pom.xml clean package -DskipTests

FROM eclipse-temurin:25-jre

WORKDIR /app

COPY --from=build /app/worker/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]