# ── Stage 1: Build ──────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# 先下載依賴（利用 Docker cache，pom.xml 沒變就不重新下載）
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

# ── Stage 2: Runtime ─────────────────────────────────────
# 註：不使用 -jre-alpine，該變體在 arm64（Apple Silicon）無對應 manifest
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# 建立必要目錄
RUN mkdir -p uploads/images backups logs

EXPOSE 8080

# Profile 由環境變數 SPRING_PROFILES_ACTIVE 控制（預設 sit）
# 本機開發請用 mvn spring-boot:run，不需透過 Docker
ENV SPRING_PROFILES_ACTIVE=sit

ENTRYPOINT ["sh", "-c", "java -jar app.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE}"]
