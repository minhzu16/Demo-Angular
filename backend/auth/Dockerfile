# Build stage - sử dụng Maven
FROM maven:3.9.5-eclipse-temurin-17 AS builder
WORKDIR /build

# Copy pom files
COPY pom.xml ./pom.xml
COPY common/pom.xml ./common/pom.xml
COPY auth/pom.xml ./auth/pom.xml

# Copy source code
COPY common ./common
COPY auth ./auth

# Build only common and auth modules với cấu hình Maven để tránh SSL issues
RUN mvn -B -DskipTests \
    -Dmaven.wagon.http.ssl.insecure=true \
    -Dmaven.wagon.http.ssl.allowall=true \
    -Dmaven.wagon.http.ignore.validity.dates=true \
    -Dmaven.wagon.http.retryHandler.count=5 \
    -N install && \
    mvn -B -DskipTests \
    -Dmaven.wagon.http.ssl.insecure=true \
    -Dmaven.wagon.http.ssl.allowall=true \
    -Dmaven.wagon.http.ignore.validity.dates=true \
    -Dmaven.wagon.http.retryHandler.count=5 \
    -f common/pom.xml install && \
    mvn -B -DskipTests \
    -Dmaven.wagon.http.ssl.insecure=true \
    -Dmaven.wagon.http.ssl.allowall=true \
    -Dmaven.wagon.http.ignore.validity.dates=true \
    -Dmaven.wagon.http.retryHandler.count=5 \
    -f auth/pom.xml package

# Runtime stage - sử dụng alpine để giảm kích thước
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Thêm timezone và curl cho healthcheck
RUN apk add --no-cache tzdata curl && \
    cp /usr/share/zoneinfo/Asia/Ho_Chi_Minh /etc/localtime && \
    echo "Asia/Ho_Chi_Minh" > /etc/timezone && \
    apk del tzdata

# Cổng và biến môi trường
EXPOSE 8082
ENV SPRING_PROFILES_ACTIVE=docker \
    JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+UseStringDeduplication"

# Copy JAR
COPY --from=builder /build/auth/target/*.jar /app/app.jar

# Healthcheck
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -fsS http://localhost:8082/actuator/health || exit 1

# Tạo non-root user để tăng security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Entrypoint
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
