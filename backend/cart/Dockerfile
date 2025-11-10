# Build stage
FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /build

# Copy Maven settings with reliable repository configuration
COPY settings.xml /root/.m2/settings.xml

# Copy POMs for caching
COPY pom.xml ./pom.xml
COPY common/pom.xml ./common/pom.xml
COPY cart/pom.xml ./cart/pom.xml

# Pre-fetch deps with retry logic
RUN mvn -B -U -DskipTests -f cart/pom.xml dependency:go-offline -am \
    -Dmaven.wagon.http.retryHandler.count=3 \
    -Dmaven.wagon.http.connectionTimeout=60000 \
    -Dmaven.wagon.http.readTimeout=60000 || true

# Copy sources
COPY common ./common
COPY cart ./cart

# Build cart module with shell-level retry for network resilience
# 0) Install parent POM to local repo (no modules)
RUN for i in 1 2 3; do \
        mvn -B -U -DskipTests -f pom.xml -N clean install \
            -Dmaven.wagon.http.retryHandler.count=3 \
            -Dmaven.wagon.http.connectionTimeout=60000 \
            -Dmaven.wagon.http.readTimeout=60000 && break || \
        (echo "Attempt $i failed, retrying..." && sleep 5); \
    done

# 1) Build and install common to local repo (dependency of cart)
RUN for i in 1 2 3; do \
        mvn -B -U -DskipTests -f common/pom.xml clean install \
            -Dmaven.wagon.http.retryHandler.count=3 \
            -Dmaven.wagon.http.connectionTimeout=60000 \
            -Dmaven.wagon.http.readTimeout=60000 && break || \
        (echo "Attempt $i failed, retrying..." && sleep 5); \
    done

# 2) Build cart module
RUN for i in 1 2 3; do \
        mvn -B -U -DskipTests -f cart/pom.xml clean package \
            -Dmaven.wagon.http.retryHandler.count=3 \
            -Dmaven.wagon.http.connectionTimeout=60000 \
            -Dmaven.wagon.http.readTimeout=60000 && break || \
        (echo "Attempt $i failed, retrying..." && sleep 5); \
    done

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
EXPOSE 8085
ENV JAVA_OPTS=""
COPY --from=builder /build/cart/target/*.jar /app/app.jar
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
