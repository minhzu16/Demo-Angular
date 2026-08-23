# Build stage - sử dụng Maven
FROM maven:3.9.5-eclipse-temurin-17 AS builder
WORKDIR /build

# Copy pom files
COPY pom.xml ./pom.xml
COPY common/pom.xml ./common/pom.xml
COPY auth/pom.xml ./auth/pom.xml
COPY product/pom.xml ./product/pom.xml

# Copy source code
COPY common ./common
COPY auth ./auth
COPY product ./product

# Build only common and product modules với cấu hình Maven để tránh SSL issues
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
    -f auth/pom.xml install && \
    mvn -B -DskipTests \
    -Dmaven.wagon.http.ssl.insecure=true \
    -Dmaven.wagon.http.ssl.allowall=true \
    -Dmaven.wagon.http.ignore.validity.dates=true \
    -Dmaven.wagon.http.retryHandler.count=5 \
    -f product/pom.xml package

# Runtime stage - sử dụng alpine để giảm kích thước
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Thêm timezone và curl cho healthcheck
RUN apk add --no-cache tzdata curl && \
    cp /usr/share/zoneinfo/Asia/Ho_Chi_Minh /etc/localtime && \
    echo "Asia/Ho_Chi_Minh" > /etc/timezone && \
    apk del tzdata

# Cổng và biến môi trường
EXPOSE 8081
ENV SPRING_PROFILES_ACTIVE=docker \
    JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+UseStringDeduplication"

# Copy JAR
COPY --from=builder /build/product/target/*.jar /app/app.jar

# Healthcheck
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -fsS http://localhost:8081/actuator/health || exit 1

# Tạo non-root user và cấp quyền ghi cho thư mục /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup \
    && mkdir -p /app/uploads \
    && chown -R appuser:appgroup /app

USER appuser

# Entrypoint
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
