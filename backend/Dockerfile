# ========================================
# Unified Dockerfile Using Pre-Built JARs
# ========================================
# This Dockerfile deploys any microservice using pre-built JAR files
# PRE-REQUISITE: JARs must be built locally first using: mvn clean install -DskipTests
# Example: docker build --build-arg SERVICE_NAME=auth --build-arg SERVICE_PORT=8082 .

FROM eclipse-temurin:17-jre-alpine AS builder

ARG SERVICE_NAME

WORKDIR /build

# Copy all service target directories
COPY auth/target/ /build/auth/ 
COPY product/target/ /build/product/
COPY order/target/ /build/order/
COPY cart/target/ /build/cart/
COPY payment/target/ /build/payment/
COPY warehouse/target/ /build/warehouse/
COPY shop/target/ /build/shop/
COPY template_storage/target/ /build/template_storage/
COPY notification/target/ /build/notification/
COPY review/target/ /build/review/
COPY analytics/target/ /build/analytics/
COPY gateway/target/ /build/gateway/


# Select the correct JAR and move it to a known location
RUN cp /build/${SERVICE_NAME}/*.jar /build/app.jar

# ========================================
# Final Runtime Image
# ========================================
FROM eclipse-temurin:17-jre-alpine

ARG SERVICE_NAME
ARG SERVICE_PORT=8080

WORKDIR /app

# Install utilities for healthcheck and timezone
RUN apk add --no-cache curl tzdata && \
    cp /usr/share/zoneinfo/Asia/Ho_Chi_Minh /etc/localtime && \
    echo "Asia/Ho_Chi_Minh" > /etc/timezone && \
    apk del tzdata

# Copy the selected JAR from builder stage
COPY --from=builder /build/app.jar /app/app.jar

# Environment variables
ENV SPRING_PROFILES_ACTIVE=docker \
    JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC" \
    SERVICE_NAME=${SERVICE_NAME} \
    SERVICE_PORT=${SERVICE_PORT}

# Expose the service port
EXPOSE ${SERVICE_PORT}

# Healthcheck
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:${SERVICE_PORT}/actuator/health || exit 1

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup && \
    chown -R appuser:appgroup /app

USER appuser

# Start the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
