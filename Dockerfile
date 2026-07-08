# 멀티스테이지 앱 이미지(Phase 18).
#  build 스테이지: JDK 21로 bootJar만 빌드(프론트·테스트 제외 — Caddy가 프론트 서빙, 테스트는 CI에서).
#  run   스테이지: JRE 21 런타임에서 비루트(appuser)로 실행.
# 시크릿은 이미지 레이어에 굽지 않는다 — LOSTARK_API_KEY / ADMIN_API_SECRET / DB·Redis 비번은
# 런타임 env(.env.prod → docker-compose)로만 주입한다(DEPLOY-04).
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle
COPY src ./src
RUN chmod +x gradlew && ./gradlew --no-daemon clean bootJar -x test

FROM eclipse-temurin:21-jre
# 비루트 런타임 — 컨테이너 탈출 시 권한 최소화.
RUN useradd -r -u 1001 appuser
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
USER appuser
EXPOSE 8080
# 컨테이너 메모리에 맞춰 힙 상한(Oracle Always Free 소형 VM 고려).
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
