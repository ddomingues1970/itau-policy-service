# ---------- Runtime only ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copia o JAR já compilado no host (mvn -DskipTests clean package)
COPY target/*.jar /app/app.jar

EXPOSE 8080
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
