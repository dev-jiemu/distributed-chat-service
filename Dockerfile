FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# pom.xml 복사 (의존성 캐싱을 위해)
COPY pom.xml .

# 의존성 다운로드
RUN mvn dependency:go-offline

# 소스 코드 복사
COPY src src

# 애플리케이션 빌드
RUN mvn package -DskipTests

# 실행 단계
FROM eclipse-temurin:17-jre

WORKDIR /app

# 빌드된 JAR 파일 복사
COPY --from=builder /app/target/distributed-chat-service-0.0.1-SNAPSHOT.jar app.jar

# JAR 파일 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
