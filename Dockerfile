FROM node:22-alpine AS frontend
WORKDIR /workspace
COPY package.json package-lock.json ./
RUN npm ci
COPY src/main/frontend src/main/frontend
RUN npm run build

FROM maven:3.9-eclipse-temurin-17 AS backend
WORKDIR /workspace
COPY pom.xml ./
RUN mvn --batch-mode dependency:go-offline
COPY config config
COPY src src
COPY --from=frontend /workspace/src/main/resources/static/js/app.js src/main/resources/static/js/app.js
RUN mvn --batch-mode -DskipTests package

# Ubuntu-based Temurin publishes both arm64 and amd64 images, so the same image
# runs on Apple Silicon Macs, Intel/Windows Docker Desktop and Linux servers.
FROM eclipse-temurin:17-jre-noble
RUN apt-get update \
    && apt-get install --yes --no-install-recommends postgresql-client wget \
    && rm -rf /var/lib/apt/lists/* \
    && addgroup --system app \
    && adduser --system --ingroup app app
WORKDIR /app
RUN mkdir -p /backups && chown app:app /backups
COPY --from=backend /workspace/target/minshuku-management-*.jar app.jar
USER app
EXPOSE 8000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
