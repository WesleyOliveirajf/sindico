# Stage 1: build
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /app
ENV MAVEN_VERSION=3.9.9
RUN apt-get update && apt-get install -y curl --no-install-recommends \
    && curl -fsSL https://dlcdn.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz \
       | tar xz -C /opt \
    && ln -s /opt/apache-maven-${MAVEN_VERSION}/bin/mvn /usr/local/bin/mvn \
    && rm -rf /var/lib/apt/lists/*
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

# Stage 2: runtime
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=builder /app/target/sindico-app-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
