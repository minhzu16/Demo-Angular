# Build stage - sử dụng maven slim để giảm kích thước
FROM maven:3.9.5-eclipse-temurin-17 AS builder
WORKDIR /build

# Copy Maven settings with reliable repository configuration
COPY settings.xml /root/.m2/settings.xml

# Copy POMs for caching 
# Sắp xếp theo thứ tự để tối ưu cache trong Docker layers
COPY pom.xml ./pom.xml
COPY common/pom.xml ./common/pom.xml
COPY auth/pom.xml ./auth/pom.xml

# Pre-fetch deps - giảm thời gian chờ
RUN mvn -B -U -DskipTests -f auth/pom.xml dependency:go-offline -am \
    -Dmaven.wagon.http.retryHandler.count=3 \
    -Dmaven.wagon.http.connectionTimeout=20000 || true

# Copy sources
COPY common ./common
COPY auth ./auth

# Build tất cả với một lệnh duy nhất để giảm kích thước image
RUN mvn -B -U -DskipTests -f auth/pom.xml clean package \
    -Dmaven.wagon.http.retryHandler.count=3 \
    -Dmaven.wagon.http.connectionTimeout=20000 || \
    mvn -B -U -DskipTests -f auth/pom.xml clean package

# Runtime stage - alpine để giảm kích thước image
FROM eclipse-temurin:17-jre
WORKDIR /app

# Thêm timezone Việt Nam
RUN apt-get update && apt-get install -y --no-install-recommends tzdata wget && \
    ln -sf /usr/share/zoneinfo/Asia/Ho_Chi_Minh /etc/localtime && \
    echo "Asia/Ho_Chi_Minh" > /etc/timezone && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Cấu hình port 
# Được sử dụng bởi Docker để exposure port
EXPOSE 8082

# Thêm các biến môi trường mặc định
ENV SPRING_PROFILES_ACTIVE=docker \
    JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

# Copy JAR file từ build stage
COPY --from=builder /build/auth/target/*.jar /app/app.jar

# Health check cho Docker Swarm/K8s
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget -q -O /dev/null http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]

