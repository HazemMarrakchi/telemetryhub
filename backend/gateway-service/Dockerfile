FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml pom.xml
COPY gateway-service/ gateway-service/
RUN mvn -B -f pom.xml -pl gateway-service -am package -Dmaven.test.skip=true

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /build/gateway-service/target/gateway-service-*.jar /app/app.jar
RUN apk add --no-cache tzdata \
    && addgroup -S app && adduser -S app -G app \
    && chown -R app:app /app
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]