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

# Usuario sem privilegios; /app/uploads precisa ser gravavel (volume de anexos).
RUN groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --no-create-home app \
    && mkdir -p /app/uploads \
    && chown -R app:app /app

COPY --from=builder --chown=app:app /app/target/sindico-app-*.jar app.jar
USER app
EXPOSE 8080

# A imagem JRE nao tem curl/wget: checa /actuator/health via /dev/tcp do bash.
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD ["bash", "-c", "exec 3<>/dev/tcp/127.0.0.1/${PORT:-8080} && printf 'GET /actuator/health HTTP/1.0\r\n\r\n' >&3 && grep -q '\"status\":\"UP\"' <&3"]

# Heap relativo ao limite do container; SerialGC e stacks menores para hosts compartilhados.
# Sobrescreva com JAVA_TOOL_OPTIONS (ex.: -Xmx512m) se precisar de um teto fixo.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=65", "-XX:+UseSerialGC", "-Xss512k", "-jar", "app.jar"]
