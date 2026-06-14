# ecommerce

Spring Boot 기반 이커머스 백엔드 애플리케이션입니다.

## 기술 스택

- Java 21
- Spring Boot 3.5
- MySQL 8.0
- Redis 7
- Docker

## 로컬 실행 (Docker)

1. .env 파일 준비
   ```bash
   cp .env.example .env
   # .env 파일을 열어 값을 채워넣으세요
   ```

2. 빌드 및 실행
   ```bash
   docker compose up --build
   ```

3. 종료
   ```bash
   docker compose down
   ```

4. 데이터까지 초기화
   ```bash
   docker compose down -v
   ```

## 헬스체크

애플리케이션 실행 후 아래 URL에서 상태를 확인할 수 있습니다.

```
http://localhost:8080/actuator/health
```
