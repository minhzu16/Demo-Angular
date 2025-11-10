# Build stage
FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /build

# Copy Maven settings with mirror
COPY settings.xml /root/.m2/settings.xml

# Copy POMs for caching
COPY pom.xml ./pom.xml
COPY common/pom.xml ./common/pom.xml
COPY auth/pom.xml ./auth/pom.xml
COPY product/pom.xml ./product/pom.xml
COPY order/pom.xml ./order/pom.xml

# Pre-fetch deps for order
RUN mvn -B -U -DskipTests -f order/pom.xml dependency:go-offline -am || true

# Copy sources
COPY common ./common
COPY auth ./auth
COPY product ./product
COPY order ./order

# Build order module with shell-level retry for network resilience
# 0) Install parent POM to local repo (no modules)
RUN for i in 1 2 3; do \
        mvn -B -U -DskipTests -f pom.xml -N clean install \
            -Dmaven.wagon.http.retryHandler.count=3 \
            -Dmaven.wagon.http.connectionTimeout=60000 \
            -Dmaven.wagon.http.readTimeout=60000 && break || \
        (echo "Attempt $i failed, retrying..." && sleep 5); \
    done

# 1) Build and install common
RUN for i in 1 2 3; do \
        mvn -B -U -DskipTests -f common/pom.xml -am clean install \
            -Dmaven.wagon.http.retryHandler.count=3 \
            -Dmaven.wagon.http.connectionTimeout=60000 \
            -Dmaven.wagon.http.readTimeout=60000 && break || \
        (echo "Attempt $i failed, retrying..." && sleep 5); \
    done

# 2) Build and install auth (order depends on auth)
RUN for i in 1 2 3; do \
        mvn -B -U -DskipTests -f auth/pom.xml -am clean install \
            -Dmaven.wagon.http.retryHandler.count=3 \
            -Dmaven.wagon.http.connectionTimeout=60000 \
            -Dmaven.wagon.http.readTimeout=60000 && break || \
        (echo "Attempt $i failed, retrying..." && sleep 5); \
    done

# 3) Build and install product (order depends on product)
RUN for i in 1 2 3; do \
        mvn -B -U -DskipTests -f product/pom.xml -am clean install \
            -Dmaven.wagon.http.retryHandler.count=3 \
            -Dmaven.wagon.http.connectionTimeout=60000 \
            -Dmaven.wagon.http.readTimeout=60000 && break || \
        (echo "Attempt $i failed, retrying..." && sleep 5); \
    done

# 4) Finally, build order module
RUN for i in 1 2 3; do \
        mvn -B -U -DskipTests -f order/pom.xml -am clean package \
            -Dmaven.wagon.http.retryHandler.count=3 \
            -Dmaven.wagon.http.connectionTimeout=60000 \
            -Dmaven.wagon.http.readTimeout=60000 && break || \
        (echo "Attempt $i failed, retrying..." && sleep 5); \
    done

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
EXPOSE 8083
ENV JAVA_OPTS=""
COPY --from=builder /build/order/target/*.jar /app/app.jar
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
