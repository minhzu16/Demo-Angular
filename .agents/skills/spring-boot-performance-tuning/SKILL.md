---
name: spring-boot-performance-tuning
description: >
  High-performance tuning for enterprise Spring Boot 3 & JPA microservices. Covers Open Session In View (OSIV)
  disabling, HikariCP connection pool optimization, Hibernate JDBC batching, N+1 query elimination, read-only
  transaction scoping, HTTP GZIP response compression, and production SQL logging discipline.
metadata:
  version: "1.0.0"
  domain: backend-performance
  source: "https://github.com/rrezartprebreza/spring-boot-skills & https://github.com/piomin/claude-ai-spring-boot"
  triggers: performance, optimization, latency, throughput, connection pool, hikari, N+1, jpa tuning
---

# Spring Boot Performance Tuning Skill

Enterprise performance tuning patterns for high-throughput, low-latency Spring Boot 3 microservices and Spring Data JPA.

---

## 1. Database & Persistence Layer (JPA & Hibernate)

### 1.1 Disable Open Session in View (OSIV)
- **Problem**: When `spring.jpa.open-in-view=true` (Spring Boot default), database connections are held across the entire HTTP request lifecycle, including during remote REST calls (Feign), serialization, and I/O. Under load, this causes rapid connection pool starvation.
- **Rule**: ALWAYS set `spring.jpa.open-in-view=false` across all microservices.
- **Fix**:
  ```properties
  spring.jpa.open-in-view=false
  ```
  ```yaml
  spring:
    jpa:
      open-in-view: false
  ```

### 1.2 Enable JDBC Batch Inserts and Updates
- **Problem**: By default, Hibernate issues a separate `INSERT` or `UPDATE` statement for each entity, creating unnecessary database roundtrips.
- **Rule**: Enable batching and statement ordering for bulk operations.
- **Configuration**:
  ```properties
  spring.jpa.properties.hibernate.jdbc.batch_size=30
  spring.jpa.properties.hibernate.order_inserts=true
  spring.jpa.properties.hibernate.order_updates=true
  spring.jpa.properties.hibernate.jdbc.batch_versioned_data=true
  ```

### 1.3 N+1 Query Prevention
- **Rule**: Never access `@OneToMany` or `@ManyToMany` lazy collections in loops without fetching them up-front.
- **Solution 1 - JOIN FETCH**:
  ```java
  @Query("SELECT o FROM OrderEntity o JOIN FETCH o.items WHERE o.id = :id")
  Optional<OrderEntity> findByIdWithItems(@Param("id") Long id);
  ```
- **Solution 2 - @EntityGraph**:
  ```java
  @EntityGraph(attributePaths = {"items", "items.product"})
  List<OrderEntity> findByUserId(Long userId);
  ```
- **Solution 3 - DTO Projection**: When only a subset of fields is needed, project directly to a record or interface to bypass entity hydration and dirty-checking overhead.

### 1.4 Read-Only Transaction Optimization
- **Rule**: Annotate all query-only service methods or classes with `@Transactional(readOnly = true)`.
- **Benefit**:
  - Hibernate disables dirty-checking snapshots on retrieved entities (saving heap memory and CPU cycles).
  - JDBC drivers can route queries to database read replicas if configured.
- **Example**:
  ```java
  @Service
  @Transactional(readOnly = true)
  public class ProductService {
      public ProductDto getById(Long id) { ... } // inherits readOnly = true

      @Transactional // overrides readOnly for writes
      public ProductDto updateProduct(...) { ... }
  }
  ```

---

## 2. HikariCP Connection Pool Sizing & Health

- **Problem**: Default HikariCP settings (pool size 10, no leak detection) can cause silent thread stalls when connection leaks occur under load.
- **Rule**: Tune connection pools based on hardware concurrency formula: `pool_size = (cpu_cores * 2) + effective_spindle_count`. Provide sensible production defaults and enable connection leak detection.
- **Configuration**:
  ```properties
  # HikariCP Production Settings
  spring.datasource.hikari.maximum-pool-size=${HIKARI_MAX_POOL_SIZE:20}
  spring.datasource.hikari.minimum-idle=${HIKARI_MIN_IDLE:5}
  spring.datasource.hikari.idle-timeout=300000
  spring.datasource.hikari.connection-timeout=20000
  spring.datasource.hikari.max-lifetime=1200000
  spring.datasource.hikari.leak-detection-threshold=60000
  ```

---

## 3. Network & HTTP Payload Compression

- **Problem**: Microservices exchanging large JSON payloads (product catalogs, order summaries) consume high network bandwidth and increase latency.
- **Rule**: Enable GZIP HTTP response compression for text and JSON MIME types above 1KB.
- **Configuration**:
  ```properties
  server.compression.enabled=true
  server.compression.mime-types=text/html,text/xml,text/plain,text/css,text/javascript,application/javascript,application/json,application/xml
  server.compression.min-response-size=1024
  ```

---

## 4. Logging & Diagnostics Discipline

- **Rule**: NEVER leave `spring.jpa.show-sql=true` active by default in production. Standard out SQL printing causes massive thread contention and disk I/O bottlenecks.
- **Configuration**:
  ```properties
  spring.jpa.show-sql=${SPRING_JPA_SHOW_SQL:false}
  spring.jpa.properties.hibernate.format_sql=${SPRING_JPA_FORMAT_SQL:false}
  ```
- **Rule**: Use parameterized SLF4J logging (`log.info("Order processed: id={}, status={}", id, status)`) instead of string concatenation (`+`).
