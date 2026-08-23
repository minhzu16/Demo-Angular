# Build stage - sử dụng Maven cache nhiều hơn
FROM maven:3.9.5-eclipse-temurin-17 AS builder
WORKDIR /build

# Đổi chiến lược copy và build để tận dụng cache
# 1. Copy settings và pom files trước
COPY settings.xml /root/.m2/settings.xml
COPY pom.xml ./pom.xml
COPY common/pom.xml ./common/pom.xml
COPY auth/pom.xml ./auth/pom.xml
COPY product/pom.xml ./product/pom.xml
COPY order/pom.xml ./order/pom.xml

# Tạo script build với retry logic
RUN echo '#!/bin/bash\n\
MVN_OPTS="-B -U -DskipTests -Dmaven.wagon.http.pool=false -Dhttp.keepAlive=false -Dmaven.wagon.http.retryHandler.count=10 -Dmaven.wagon.http.connectionTimeout=90000 -Dmaven.wagon.http.readTimeout=90000"\n\
\n\
run_with_retry() {\n\
  cmd="$1"\n\
  max_retries=3\n\
  retries=0\n\
  echo "Running: $cmd"\n\
  while [ $retries -lt $max_retries ]; do\n\
    if eval "$cmd"; then\n\
      return 0\n\
    else\n\
      retries=$((retries+1))\n\
      echo "Command failed, retry $retries of $max_retries"\n\
      sleep 2\n\
    fi\n\
  done\n\
  return 1\n\
}\n\
\n\
# Run each build step with retry\n\
run_with_retry "mvn $MVN_OPTS -f pom.xml -N install" || exit 1\n\
run_with_retry "mvn $MVN_OPTS -f common/pom.xml clean install" || exit 1\n\
run_with_retry "mvn $MVN_OPTS -f auth/pom.xml clean install" || exit 1\n\
run_with_retry "mvn $MVN_OPTS -f product/pom.xml clean install" || exit 1\n\
run_with_retry "mvn $MVN_OPTS -f order/pom.xml dependency:go-offline" || exit 1\n\
' > /build/build-with-retry.sh && chmod +x /build/build-with-retry.sh

# 2. Pre-fetch dependencies với retry script
RUN /build/build-with-retry.sh

# 3. Copy source code
COPY common ./common
COPY auth ./auth
COPY product ./product
COPY order ./order

# 4. Build final JAR
RUN echo '#!/bin/bash\n\
MVN_OPTS="-B -DskipTests -Dmaven.wagon.http.pool=false -Dhttp.keepAlive=false -Dmaven.wagon.http.retryHandler.count=10 -Dmaven.wagon.http.connectionTimeout=90000 -Dmaven.wagon.http.readTimeout=90000"\n\
\n\
run_with_retry() {\n\
  cmd="$1"\n\
  max_retries=3\n\
  retries=0\n\
  echo "Running: $cmd"\n\
  while [ $retries -lt $max_retries ]; do\n\
    if eval "$cmd"; then\n\
      return 0\n\
    else\n\
      retries=$((retries+1))\n\
      echo "Command failed, retry $retries of $max_retries"\n\
      sleep 2\n\
    fi\n\
  done\n\
  return 1\n\
}\n\
\n\
# Build all modules\n\
run_with_retry "mvn $MVN_OPTS -f common/pom.xml install" || exit 1\n\
run_with_retry "mvn $MVN_OPTS -f auth/pom.xml install" || exit 1\n\
run_with_retry "mvn $MVN_OPTS -f product/pom.xml install" || exit 1\n\
run_with_retry "mvn $MVN_OPTS -f order/pom.xml package" || exit 1\n\
' > /build/build-jar.sh && chmod +x /build/build-jar.sh && /build/build-jar.sh

# Runtime stage - sử dụng alpine để giảm kích thước
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Thêm timezone và curl cho healthcheck
RUN apk add --no-cache tzdata curl && \
    cp /usr/share/zoneinfo/Asia/Ho_Chi_Minh /etc/localtime && \
    echo "Asia/Ho_Chi_Minh" > /etc/timezone && \
    apk del tzdata

# Cổng và biến môi trường
EXPOSE 8083
ENV SPRING_PROFILES_ACTIVE=docker \
    JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+UseStringDeduplication"

# Copy JAR
COPY --from=builder /build/order/target/*.jar /app/app.jar

# Healthcheck
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8083/actuator/health || exit 1

# Tạo non-root user để tăng security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Entrypoint
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
