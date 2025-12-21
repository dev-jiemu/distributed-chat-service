### Memo
1. Docker-compose 관련

`--scale` 옵션 관련

```yaml
chat-server:
  build: .
  # container_name 제거 (scale 사용시 충돌)
  ports:
    - "8080"  # 랜덤 포트 매핑
  environment:
    # SERVER_ID는 애플리케이션에서 자동 생성하도록
    SPRING_RABBITMQ_HOST: rabbitmq
    SPRING_REDIS_HOST: redis
    SPRING_PROFILES_ACTIVE: docker
  depends_on:
    rabbitmq:
      condition: service_healthy
    redis:
      condition: service_healthy
  networks:
    - chat_network
```
```shell
docker-compose up --scale chat-server=3
```

쿠버네티스 환경이면 굳이 scale 옵션 안써도 될듯 하지만...? ㅇㅂㅇ?

+) nginx 등 설정할때 (트래픽 분산)
```shell
nginx:
  image: nginx:alpine
  ports:
    - "80:80"
  volumes:
    - ./nginx.conf:/etc/nginx/nginx.conf:ro
  depends_on:
    - chat-server
  networks:
    - chat_network

chat-server:
  build: .
  # 외부 포트 노출 제거 (nginx를 통해서만 접근)
  environment:
    SPRING_RABBITMQ_HOST: rabbitmq
    SPRING_REDIS_HOST: redis
    SPRING_PROFILES_ACTIVE: docker
  depends_on:
    rabbitmq:
      condition: service_healthy
    redis:
      condition: service_healthy
  networks:
    - chat_network
```
```
upstream chat-servers {
    server chat-server-1:8080;
    server chat-server-2:8080;
    server chat-server-3:8080;
}

server {
    listen 80;
    location / {
        proxy_pass http://chat-servers;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```