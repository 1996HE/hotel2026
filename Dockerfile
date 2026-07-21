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

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=backend /workspace/target/minshuku-management-*.jar app.jar
USER app
EXPOSE 8000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
