FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY . .
RUN --mount=type=cache,target=/root/.m2 mvn -B -f pom.xml -pl maintenance-service -am package -Dmaven.test.skip=true

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /build/maintenance-service/target/maintenance-service-*.jar /app/app.jar
RUN apk add --no-cache tzdata \
    && addgroup -S app && adduser -S app -G app \
    && chown -R app:app /app
USER app
EXPOSE 8086
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]