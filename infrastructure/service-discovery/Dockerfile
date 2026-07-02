FROM eclipse-temurin:25.0.3_9-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app
COPY target/*.jar app.jar
RUN chown spring:spring app.jar
USER spring
EXPOSE 8761
HEALTHCHECK --interval=15s --timeout=5s --retries=3 \
  CMD wget -qO- http://localhost:8761/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:InitialRAMPercentage=50.0", "-jar", "app.jar"]
