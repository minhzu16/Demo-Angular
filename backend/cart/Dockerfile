# Build stage
FROM maven:3.9.5-eclipse-temurin-17 AS builder
WORKDIR /build

# Copy Maven settings with reliable repository configuration
COPY settings.xml /root/.m2/settings.xml

# Copy POMs for caching
COPY pom.xml ./pom.xml
COPY common/pom.xml ./common/pom.xml
COPY cart/pom.xml ./cart/pom.xml

# Pre-fetch deps with retry logic - tối ưu bằng cách giảm các đối số không cần thiết
RUN mvn -B -U -DskipTests -f pom.xml -pl cart -am dependency:go-offline \
    -Dmaven.wagon.http.retryHandler.count=3 \
    -Dmaven.wagon.http.connectionTimeout=20000 || true

# Copy sources - chỉ copy các thư mục cần thiết
COPY common ./common
COPY cart ./cart

# Build với một lệnh duy nhất để giảm kích thước image
RUN mvn -B -U -DskipTests -f pom.xml -pl cart -am clean package \
    -Dmaven.wagon.http.retryHandler.count=3 \
    -Dmaven.wagon.http.connectionTimeout=20000 || \
    mvn -B -U -DskipTests -f pom.xml -pl cart -am clean package

# Runtime stage - sử dụng alpine để giảm kích thước image
FROM eclipse-temurin:17-jre
WORKDIR /app

# Thêm timezone Việt Nam
RUN apt-get update && apt-get install -y --no-install-recommends tzdata wget && \
    ln -sf /usr/share/zoneinfo/Asia/Ho_Chi_Minh /etc/localtime && \
    echo "Asia/Ho_Chi_Minh" > /etc/timezone && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Port
EXPOSE 8085

# Thêm các biến môi trường mặc định 
ENV SPRING_PROFILES_ACTIVE=docker \
    JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

COPY --from=builder /build/cart/target/*.jar /app/app.jar

# Health check cho Docker
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget -q -O /dev/null http://localhost:8085/actuator/health || exit 1

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]


