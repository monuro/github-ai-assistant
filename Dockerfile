# GitHub AI Assistant Docker Image
# 使用方法: docker build -t gh-ai . && docker run -it gh-ai explain "git rebase"

FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build
COPY pom.xml .
COPY src ./src

RUN apk add --no-cache maven && \
    mvn package -DskipTests -q && \
    mv target/github-ai-assistant-*.jar app.jar

# 运行时镜像
FROM eclipse-temurin:21-jre-alpine

LABEL org.opencontainers.image.source="https://github.com/JackyST0/github-ai-assistant"
LABEL org.opencontainers.image.description="AI-powered GitHub CLI assistant"
LABEL org.opencontainers.image.licenses="MIT"

WORKDIR /app
COPY --from=builder /build/app.jar .

# 默认工作目录
WORKDIR /workspace

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
CMD ["--help"]
