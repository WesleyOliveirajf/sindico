# Stage 1: build — usa imagem oficial com Maven + JDK 25 pre-instalados
FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

# Stage 2: runtime — apenas JRE (imagem menor)
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=builder /app/target/sindico-app-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
