# 🛒 hhplus-ecommerce

> 잔액 충전, 상품 조회, 선착순 쿠폰, 주문/결제 기능을 제공하는 이커머스 주문 서비스

## 📋 Table of Contents

- [Introduction](#-introduction)
- [Tech Stack](#-tech-stack)
- [Getting Started](#-getting-started)
- [Documentation](#-documentation)
- [Project Structure](#-project-structure)

## 🎯 Introduction

이 프로젝트는 e-커머스 상품 주문 서비스입니다.  
다수의 인스턴스 환경에서도 정합성을 보장하는 **동시성 제어**, 결제와 분리된 **이벤트 기반 비동기 처리**,  
그리고 유지보수성을 고려한 **DDD 기반 도메인 설계**를 핵심 목표로 구현하였습니다.

**주요 기능**
- 잔액 충전 / 조회
- 상품 목록 조회 및 인기 상품 조회 (최근 3일 TOP 5)
- 선착순 쿠폰 발급 및 조회
- 주문 / 결제 (잔액 기반, 쿠폰 적용)
- 결제 완료 시 데이터 플랫폼 실시간 전송 (Kafka 기반 비동기)

## 🛠 Tech Stack

| 분류 | 기술 |
|------|------|
| **언어** | Java 21 |
| **프레임워크** | Spring Boot 3.5.14 |
| **데이터베이스** | MySQL 8.0 |
| **메시징** | Apache Kafka (KRaft 모드) |
| **동시성 제어** | Pessimistic Lock (JPA) |
| **컨테이너** | Docker & Docker Compose |
| **빌드** | Gradle |

## 🚀 Getting Started

### Prerequisites

- Java 21 이상
- Docker & Docker Compose
- Git

### Local Environment Setup

1. **Clone Repository**
   ```bash
   git clone https://github.com/{username}/hhplus-ecommerce.git
   cd hhplus-ecommerce
   ```

2. **환경변수 파일 준비**
   ```bash
   cp .env.example .env
   # .env 파일을 열어 DB 비밀번호 등 값을 채워넣으세요
   ```

3. **인프라 컨테이너 실행 (MySQL + Kafka)**
   ```bash
   docker compose up -d
   ```

4. **애플리케이션 실행**
   ```bash
   ./gradlew bootRun --args='--spring.profiles.active=docker'
   ```

5. **전체 환경 한 번에 실행 (앱 포함)**
   ```bash
   docker compose up --build
   ```

### API 확인

실행 후 Swagger UI에서 API를 확인할 수 있습니다.
```
http://localhost:8080/swagger-ui/index.html
```

## 📚 Documentation

### 📖 Core Documents
- [ERD](./docs/erd.md)
- [API 명세서](./docs/api-spec.md)
- [아키텍처 및 핵심 설계 결정](./docs/architecture.md)
- [코드 / DB 컨벤션](./docs/convention.md)

## 📁 Project Structure

```
src/main/java/com/ecommerce/ecommerce/
├── user/                 # 사용자 / 잔액
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── domain/
│   └── dto/
├── product/              # 상품 / 재고 / 인기 상품
│   └── ...
├── coupon/               # 선착순 쿠폰
│   └── ...
├── order/                # 주문 / 결제 / 이벤트 발행
│   ├── ...
│   └── event/
│       ├── OrderCompletedEvent.java
│       ├── OrderEventProducer.java      # Kafka Producer
│       └── DataPlatformConsumer.java    # Kafka Consumer (Mock)
└── common/
    ├── config/
    ├── exception/
    └── response/
```
