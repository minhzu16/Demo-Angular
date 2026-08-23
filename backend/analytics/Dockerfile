# Build stage - sử dụng Maven cache nhiều hơn
FROM maven:3.9.5-eclipse-temurin-17 AS builder
WORKDIR /build

# Đổi chiến lược copy và build để tận dụng cache
# 1. Copy settings và pom files trước
COPY settings.xml /root/.m2/settings.xml
COPY pom.xml ./pom.xml
COPY common/pom.xml ./common/pom.xml
COPY analytics/pom.xml ./analytics/pom.xml

# 2. Pre-fetch dependencies (chạy offline) với các tham số tối ưu
RUN mvn -B -U -DskipTests -f pom.xml -N install && \
    mvn -B -U -DskipTests -f common/pom.xml dependency:go-offline && \
    mvn -B -U -DskipTests -f analytics/pom.xml dependency:go-offline \
    -Dmaven.wagon.http.retryHandler.count=3 \
    -Dmaven.wagon.http.connectionTimeout=30000

# 3. Copy source code
COPY common ./common
COPY analytics ./analytics

# 4. Build modules với một lệnh đơn giản
RUN mvn -B -DskipTests -f common/pom.xml install && \
    mvn -B -DskipTests -f analytics/pom.xml package

# Runtime stage - sử dụng alpine để giảm kích thước
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Thêm timezone và curl cho healthcheck
RUN apk add --no-cache tzdata curl && \
    cp /usr/share/zoneinfo/Asia/Ho_Chi_Minh /etc/localtime && \
    echo "Asia/Ho_Chi_Minh" > /etc/timezone && \
    apk del tzdata

# Cổng và biến môi trường
EXPOSE 8095
ENV SPRING_PROFILES_ACTIVE=docker \
    JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+UseStringDeduplication"

# Copy JAR
COPY --from=builder /build/analytics/target/*.jar /app/app.jar

# Healthcheck
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8095/actuator/health || exit 1

# Tạo non-root user để tăng security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Entrypoint
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
