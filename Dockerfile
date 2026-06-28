# syntax=docker/dockerfile:1

# ===== build stage =====
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Gradle 래퍼/설정 먼저 복사 → 의존성 레이어 캐시
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

# 소스 복사 후 bootJar (테스트는 CI에서 수행 → 이미지 빌드에서는 제외)
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar -x test

# ===== runtime stage =====
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

# non-root 실행
RUN groupadd --system app && useradd --system --gid app app

COPY --from=build /workspace/build/libs/*.jar app.jar
USER app

EXPOSE 8080
# 컨테이너 메모리 인지(t3.small 2GB). 프로필은 런타임 env(SPRING_PROFILES_ACTIVE)로 주입.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
