FROM eclipse-temurin:25-jre-alpine AS runtime

RUN addgroup -S app && adduser -S app -G app

WORKDIR /app

# curl for health checks
RUN apk add --no-cache curl

COPY advert-market-app/build/libs/advert-market-app-*.jar app.jar

RUN chown -R app:app /app
USER app

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["java", \
    "-XX:+UseZGC", "-XX:+ZGenerational", \
    "-Xmx512m", "-Xms512m", \
    "--enable-preview", \
    "-jar", "app.jar"]