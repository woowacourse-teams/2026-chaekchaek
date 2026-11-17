FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --home-dir /app incident \
    && mkdir -p /app/data && chown -R 10001:10001 /app
WORKDIR /app
COPY --chown=10001:10001 incident-analyzer.jar /app/incident-analyzer.jar
USER 10001:10001
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=3 \
    CMD curl -fsS --max-time 3 http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/incident-analyzer.jar"]
