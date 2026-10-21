# syntax=docker/dockerfile:1

# ---------- Stage 1: build (JDK + Maven wrapper) ----------
FROM eclipse-temurin:25-jdk-noble AS build
WORKDIR /workspace

# 1) Salin HANYA file build dulu → layer dependency di-cache selama pom.xml tidak berubah
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw          # jaga-jaga kalau mvnw ber-line-ending Windows (CRLF)
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline

# 2) Baru salin source code → perubahan kode tidak mengunduh ulang dependency
COPY src src
COPY lombok.config ./
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q -DskipTests package

# 3) Pecah jar menjadi layer: library (jarang berubah) terpisah dari kode aplikasi (sering berubah)
RUN cp target/*.jar application.jar \
    && java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# ---------- Stage 2: runtime (JRE saja) ----------
FROM eclipse-temurin:25-jre-noble
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app --no-create-home app

# Urutan COPY = urutan layer: yang paling jarang berubah di atas
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

USER app
EXPOSE 8080

# Sadar batas memori container + mati bersih kalau kehabisan memori (Kubernetes akan me-restart)
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError" \
    SPRING_PROFILES_ACTIVE=prod

# Exec-form (array): java menjadi PID 1 → menerima SIGTERM → graceful shutdown berjalan
ENTRYPOINT ["java", "-jar", "application.jar"]