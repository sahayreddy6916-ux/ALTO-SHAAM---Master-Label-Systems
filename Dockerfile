FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

# Run as an unprivileged user.
RUN groupadd --system app && useradd --system --gid app --home /app app \
    && mkdir -p /app/data/generated && chown -R app:app /app
COPY --from=build --chown=app:app /app/target/label-system-1.0.0.jar /app/app.jar
USER app

EXPOSE 8081
ENV PORT=8081
ENV DB_URL=jdbc:postgresql://postgres:5432/label_system
ENV DB_USERNAME=postgres
# DB_PASSWORD has no default on purpose: pass it at runtime (docker run -e / compose / secret).
ENV CORS_ORIGINS=http://localhost:5173
ENV LABEL_STORAGE_DIR=/app/data/generated
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -Djava.awt.headless=true"

# Health endpoint: http://<host>:8081/actuator/health
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
