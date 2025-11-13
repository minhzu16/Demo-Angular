FROM eclipse-temurin:17-jre

WORKDIR /app

# Thêm wget và các công cụ mạng để debug kết nối
RUN apt-get update && apt-get install -y --no-install-recommends \
    tzdata \
    wget \
    curl \
    netcat-traditional \
    iputils-ping \
    && ln -sf /usr/share/zoneinfo/Asia/Ho_Chi_Minh /etc/localtime \
    && echo "Asia/Ho_Chi_Minh" > /etc/timezone \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

# Tạo script khởi động với chờ đợi MySQL sẵn sàng
RUN echo '#!/bin/bash \n\
echo "Waiting for MySQL to be ready..." \n\
attempts=0 \n\
max_attempts=30 \n\
while ! nc -z mysql 3306 && [ $attempts -lt $max_attempts ]; do \n\
  attempts=$((attempts+1)) \n\
  echo "MySQL not ready yet. Waiting... ($attempts/$max_attempts)" \n\
  sleep 1 \n\
done \n\
if [ $attempts -eq $max_attempts ]; then \n\
  echo "MySQL not available after $max_attempts attempts, printing debug info:" \n\
  ping -c 3 mysql \n\
  echo "=== MySQL container info ===" \n\
  wget -qO- http://host.docker.internal:9090/api/containers/mysql/json || true \n\
else \n\
  echo "MySQL is ready. Starting application..." \n\
fi \n\
java $JAVA_OPTS -jar /app/app.jar \n\
' > /app/startup.sh && chmod +x /app/startup.sh

EXPOSE 8081

# Các biến môi trường mặc định
ENV SPRING_PROFILES_ACTIVE=docker \
    JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

COPY target/*.jar /app/app.jar

# Health check với lệnh curl
HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["/app/startup.sh"]
