# 코드 / DB 컨벤션

이 프로젝트에서 일관되게 따르는 네이밍 및 구조 규칙입니다.

---

## 패키지 구조 (DDD 패턴)

도메인(`user`, `product`, `coupon`, `order`) 별로 패키지를 나누고, 각 도메인 내부는 계층별로 구성합니다.

```
src/main/java/com/ecommerce/ecommerce/
├── user/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── domain/
│   └── dto/
├── product/
│   └── ... (동일 구조)
├── coupon/
│   └── ... (동일 구조)
├── order/
│   └── ... (동일 구조)
└── common/
    ├── config/
    ├── exception/
    └── response/
```

**규칙**
- 도메인 간 직접 참조는 최소화한다. 필요하면 `order`가 `product`, `user`, `coupon`의 Service를 호출하는 방향으로만 의존한다 (역방향 의존 금지)
- 공통 예외, 공통 응답 포맷, 설정 클래스는 `common` 패키지에 둔다

---

## 클래스 네이밍

| 계층 | 네이밍 규칙 | 예시 |
|------|-------------|------|
| Entity | 단수형, PascalCase | `Product`, `OrderItem` |
| Repository | `{Entity}Repository` | `ProductRepository` |
| Service | `{Entity}Service` | `OrderService` |
| Controller | `{Entity}Controller` | `OrderController` |
| Request DTO | `{Entity}{Action}Request` | `OrderCreateRequest` |
| Response DTO | `{Entity}Response` | `OrderResponse` |
| Custom Exception | `{Domain}{Reason}Exception` | `OutOfStockException` |

---

## 데이터베이스 컨벤션

### 테이블명
- 복수형, snake_case 사용
- 예: `users`, `products`, `order_items`, `user_coupons`

### 컬럼명
- snake_case 사용
- 기본 키는 `{table_singular}_id` 형식 → `user_id`, `product_id`, `order_id`
- 외래 키는 참조하는 테이블의 PK명과 동일하게 → `orders.user_id` → `users.user_id`
- 생성/수정 시각은 `created_at`, `updated_at`으로 통일

### 제약 조건
- 모든 PK는 `AUTO_INCREMENT` (bigint)
- 필수 값은 `NOT NULL`
- 중복 불가 값(이메일 등)은 `UNIQUE`
- 금액/가격 관련 컬럼은 `DECIMAL` 타입 사용 (부동소수점 오차 방지)

### 인덱스
- FK 컬럼에는 기본적으로 인덱스 생성
- 조회가 빈번한 컬럼(`order_items.product_id`, `orders.ordered_at` 등)에 인덱스 추가
- 복합 조건으로 자주 조회되는 경우 복합 인덱스 고려

---

## API 컨벤션

- URL은 명사형, 복수형 사용 (`/api/v1/products`, `/api/v1/orders`)
- 버전은 `/api/v1` prefix로 관리
- HTTP 메서드 의미에 맞게 사용
  - `GET`: 조회
  - `POST`: 생성
  - `PATCH`: 부분 수정 (잔액 충전 등)
  - `PUT`: 전체 수정
  - `DELETE`: 삭제
- 응답은 항상 JSON
- 에러 응답은 `docs/api-spec.md`의 공통 에러 형식을 따른다

---

## 트랜잭션 규칙

- 여러 테이블에 걸친 쓰기 작업(주문 생성 + 재고 차감 + 잔액 차감)은 **하나의 `@Transactional` 메서드** 안에서 처리한다
- 외부 API 호출(데이터 플랫폼 전송 등)은 트랜잭션 내부에 포함시키지 않는다 — 메시지 큐를 통해 트랜잭션 커밋 이후 비동기로 처리한다

---

## 테스트 컨벤션

- 테스트 메서드명: `메서드명_상황_기대결과` (예: `decreaseStock_재고부족_예외발생`)
- Service 단위 테스트: `@ExtendWith(MockitoExtension.class)` + `@Mock` / `@InjectMocks`
- Controller 단위 테스트: `@WebMvcTest` + `@MockBean` + `MockMvc`
- 동시성 테스트: `ExecutorService` + `CountDownLatch`를 사용해 다중 스레드 환경 시뮬레이션
