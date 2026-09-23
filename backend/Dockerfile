FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /workspace
COPY . .
RUN chmod +x mvnw && ./mvnw --batch-mode -DskipTests package

FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S -g 10001 pantry && adduser -S -u 10001 -G pantry pantry
WORKDIR /app
COPY --from=build /workspace/target/pantry-backend-*.jar app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
