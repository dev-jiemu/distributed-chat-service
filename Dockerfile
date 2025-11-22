FROM openjdk:17-jdk-slim

WORKDIR /app

# Maven wrapper 복사
COPY mvnw .
COPY .mvn .mvn

# pom.xml 복사 (의존성 캐싱을 위해)
COPY pom.xml .

# 의존성 다운로드
RUN ./mvnw dependency:go-offline

# 소스 코드 복사
COPY src src

# 애플리케이션 빌드
RUN ./mvnw package -DskipTests

# JAR 파일 실행
ENTRYPOINT ["java", "-jar", "target/distributed-chat-service-0.0.1-SNAPSHOT.jar"]
