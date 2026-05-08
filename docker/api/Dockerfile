FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /app

COPY . .

RUN sed -i 's/\r$//' mvnw
RUN chmod +x mvnw

RUN ./mvnw clean install -N
RUN ./mvnw -pl shared install
RUN ./mvnw -f api/pom.xml clean package -DskipTests

FROM eclipse-temurin:25-jre

WORKDIR /app

COPY --from=build /app/api/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
