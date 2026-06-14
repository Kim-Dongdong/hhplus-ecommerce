# 아키텍처 및 핵심 설계 결정

이 문서는 시스템 구조와, 특히 동시성/통계/외부 연동에 대한 설계 결정과 그 이유를 정리합니다.

> 📌 **현재 단계: STEP02 (서버 구조 설계)**
> 이 단계의 목표는 "확장성과 유지보수성을 고려한 서버 구조 설계"이다.
> 데이터 플랫폼 전송 구조는 추후 확장성을 고려해 **Kafka를 우선 도입**했다 (Producer-Consumer 구조 학습 목적).
> Redis 캐싱(STEP06~07), 분산락(STEP05~06)은 이후 단계의 학습 주제이므로,
> 이번 단계에서는 **MySQL + Kafka 조합으로 동작하는 구조를 우선 완성**하고,
> Redis 관련 항목은 "추후 확장 고려 사항"으로만 설계 의도를 남겨둔다.

---

## 1. 전체 구조

```
Client
  │
  ▼
Spring Boot Application (다중 인스턴스 가능)
  │
  ├── MySQL  (사용자, 상품, 주문, 쿠폰, 통계 데이터)
  └── Kafka  (주문 완료 이벤트 발행/구독 → 데이터 플랫폼 전송)
```

애플리케이션이 다중 인스턴스로 동작할 수 있다는 전제이므로, **인스턴스 간 공유되는 상태(재고, 쿠폰 수량 등)는 애플리케이션 메모리가 아닌 DB 레벨에서 관리**합니다. (Redis 캐싱/분산락은 STEP06~07에서 추가 도입 예정)

---

## 2. 도메인 구조 (DDD)

| 도메인 | 책임 |
|--------|------|
| `user` | 사용자 정보, 잔액 충전/조회 |
| `product` | 상품 정보, 재고 관리, 인기 상품 조회 |
| `coupon` | 쿠폰 발급, 보유 쿠폰 조회, 쿠폰 검증 |
| `order` | 주문 생성, 결제 처리, 데이터 플랫폼 전송 |

`order` 도메인이 나머지 도메인의 Service를 호출하는 구조입니다. (주문 생성 시 `product`의 재고 차감, `user`의 잔액 차감, `coupon`의 쿠폰 사용 처리를 모두 호출)

---

## 3. 동시성 처리 전략

### 3-1. 재고 차감 (주문 / 결제)

**문제**: 재고가 1개 남은 상품에 동시에 100개의 주문 요청이 들어오면, 모든 요청이 "재고 1개 있음"으로 읽고 차감을 시도해 재고가 음수가 될 수 있다.

**해결 방안: 비관적 락 (Pessimistic Lock)**

재고는 충돌 빈도가 높을 수 있으므로, 재고를 조회하는 시점에 row 단위로 락을 걸어 다른 트랜잭션이 동시에 접근하지 못하게 한다. DB 레벨의 락이므로 다중 인스턴스 환경에서도 별도 인프라(Redis 등) 없이 정합성이 보장된다.

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM Product p WHERE p.id = :productId")
Optional<Product> findByIdWithLock(@Param("productId") Long productId);
```

```java
@Transactional
public void decreaseStock(Long productId, int quantity) {
    Product product = productRepository.findByIdWithLock(productId)
        .orElseThrow(() -> new ProductNotFoundException(productId));

    if (product.getStock() < quantity) {
        throw new OutOfStockException(productId);
    }

    product.decreaseStock(quantity);
}
```

> 비관적 락은 트랜잭션이 끝날 때까지 락을 유지하므로, 주문 트랜잭션의 범위를 최소화해야 한다. 외부 API 호출 등 느린 작업은 트랜잭션 밖에서 처리한다 (→ 6번 항목 참고).

**대안으로 고려했던 방식**
- **낙관적 락**: 충돌이 드문 경우 성능이 좋지만, 인기 상품처럼 충돌이 빈번한 경우 재시도 로직이 많이 발생해 오히려 비효율적일 수 있어 채택하지 않음
- **Redis 분산 락**: 다중 인스턴스 환경에서의 락 전략으로 고려할 수 있으나, 이번 단계에서는 인프라를 추가하지 않고 DB 락으로 동시성 문제를 해결한다. STEP05~06(동시성 제어 / 분산락) 단계에서 Redis 기반 분산락으로 비교·확장할 계획

---

### 3-2. 선착순 쿠폰 발급

**문제**: 쿠폰 수량이 100개일 때, 동시에 101번째 이상의 요청이 들어오면 발급 수량이 초과될 수 있다.

**해결 방안**: 재고와 동일하게 비관적 락으로 `coupons.issued_quantity`를 증가시키며, 증가 전에 `total_quantity`와 비교한다.

```java
@Transactional
public UserCoupon issueCoupon(Long couponId, Long userId) {
    Coupon coupon = couponRepository.findByIdWithLock(couponId)
        .orElseThrow(() -> new CouponNotFoundException(couponId));

    if (coupon.isExpired()) {
        throw new CouponExpiredException(couponId);
    }
    if (coupon.getIssuedQuantity() >= coupon.getTotalQuantity()) {
        throw new CouponOutOfStockException(couponId);
    }
    if (userCouponRepository.existsByUserIdAndCouponId(userId, couponId)) {
        throw new CouponAlreadyIssuedException(couponId, userId);
    }

    coupon.increaseIssuedQuantity();
    return userCouponRepository.save(UserCoupon.issue(userId, coupon));
}
```

> Redis의 원자적 연산(`INCR`)을 활용한 발급 방식은 STEP06~07(분산락/캐싱) 단계에서 다룰 주제로 남겨둔다. 현재 단계에서는 DB 락으로 정합성을 보장한다.

---

## 4. 잔액 충전 / 차감

잔액 변경도 동시 요청 시 정합성이 깨질 수 있는 영역이다. 재고와 동일하게 비관적 락을 적용한다.

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<User> findByIdWithLock(Long userId);
```

주문/결제 트랜잭션 안에서 `재고 차감 → 쿠폰 처리 → 잔액 차감 → 주문 저장`이 모두 성공해야 커밋되며, 하나라도 실패하면 전체 롤백된다.

---

## 5. 인기 상품 통계 (최근 3일 TOP 5)

**문제**: `order_items` 테이블이 커질수록, 매 요청마다 전체 집계 쿼리를 실행하면 응답 속도가 느려진다.

**현재 단계 채택 방안: 인덱스 기반 집계 쿼리**

`order_items.product_id`, `orders.ordered_at`에 인덱스를 걸어, 최근 3일 범위로 `GROUP BY product_id` 집계 쿼리를 실행한다.

```java
@Query("""
    SELECT oi.product.id AS productId, SUM(oi.quantity) AS salesCount
    FROM OrderItem oi
    WHERE oi.order.orderedAt >= :since
    GROUP BY oi.product.id
    ORDER BY SUM(oi.quantity) DESC
""")
List<TopProductDto> findTopProductsSince(LocalDateTime since, Pageable pageable);
```

이 방식은 데이터가 일정 규모 이상으로 커지기 전까지는 충분히 빠르며, 추가 인프라 없이 단순하게 정확한 결과를 제공한다.

**추후 개선 방향 (STEP06~07 연계)**

- **Redis 캐싱**: 위 집계 쿼리 결과를 일정 주기(예: 1시간)로 Redis에 캐싱하고, `/api/v1/products/top` 요청은 캐시에서 조회. 데이터가 많아질수록 쿼리 비용 대비 캐시 조회 비용이 크게 절감됨
- **Redis Sorted Set 활용**: 주문 발생 시점마다 `ZINCRBY`로 상품별 판매량을 갱신해두면, 별도 집계 쿼리 없이 `ZREVRANGE`로 실시간 랭킹을 즉시 조회 가능 (STEP07 "Redis를 활용한 캐싱"의 학습 목표와 직접 연결)
- **별도 통계 테이블**: 주문 완료 시점마다 `product_sales_stats` 테이블을 함께 업데이트. 다만 주문 트랜잭션이 무거워지고 락 경합이 생길 수 있어, 이벤트 기반 비동기 업데이트(STEP08 "이벤트 기반 아키텍처")와 함께 적용하는 것이 더 적합

---

## 6. 결제 성공 시 데이터 플랫폼 전송

**문제**: 결제 트랜잭션 내부에서 외부 데이터 플랫폼에 직접 HTTP 요청을 보내면, 외부 시스템이 느리거나 장애가 났을 때 결제 자체가 실패하거나 지연될 수 있다.

**채택 방안: Kafka 기반 비동기 전송**

```
[결제 트랜잭션 커밋 완료]
        │
        ▼
주문 완료 이벤트 발행 (ApplicationEventPublisher, AFTER_COMMIT)
        │
        ▼
OrderEventProducer → Kafka Topic("order-completed")
        │
        ▼
DataPlatformConsumer(Kafka Listener) → 데이터 플랫폼 전송 (Mock)
```

- 결제 트랜잭션이 커밋된 **이후에** Kafka로 메시지를 발행한다 (`@TransactionalEventListener(phase = AFTER_COMMIT)` → `OrderEventProducer`). 결제 자체는 Kafka 상태와 무관하게 항상 성공해야 한다
- 데이터 플랫폼 전송은 별도 Consumer(`DataPlatformConsumer`)가 담당하며, 실제 외부 API 대신 Mock(로그 출력)으로 구현한다
- Kafka를 사용함으로써 데이터 플랫폼이 느려지거나 다운되어도 메시지가 토픽에 쌓여있다가 복구 시 처리되므로, 결제 흐름과 완전히 분리된다

```java
// 1. 결제 트랜잭션 커밋 후 이벤트 수신 → Kafka 발행
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handleOrderCompleted(OrderCompletedEvent event) {
    orderEventProducer.send(event.toPayload());
}
```

```java
// 2. Kafka Producer
@Component
@RequiredArgsConstructor
public class OrderEventProducer {
    private static final String TOPIC = "order-completed";
    private final KafkaTemplate<String, OrderCompletedPayload> kafkaTemplate;

    public void send(OrderCompletedPayload payload) {
        kafkaTemplate.send(TOPIC, String.valueOf(payload.orderId()), payload);
    }
}
```

```java
// 3. Kafka Consumer - 데이터 플랫폼 전송 (Mock)
@Component
public class DataPlatformConsumer {
    private static final Logger log = LoggerFactory.getLogger(DataPlatformConsumer.class);

    @KafkaListener(topics = "order-completed", groupId = "ecommerce-data-platform")
    public void consume(OrderCompletedPayload payload) {
        log.info("[데이터 플랫폼 전송] {}", payload); // Mock
    }
}
```

**다중 인스턴스 환경에서의 동작**

여러 `app` 인스턴스가 동시에 주문을 처리해도, 각자 Kafka Producer로 메시지를 발행하기만 하면 된다. Consumer는 동일한 `groupId`로 묶여 있으므로, 파티션 단위로 분산 처리되어 메시지가 중복 소비되지 않는다.

**현재 단계에서의 구성**

- Kafka는 KRaft 모드(Zookeeper 불필요)로 단일 브로커, 단일 파티션 구성으로 시작한다
- 토픽: `order-completed`
- 운영 환경에서는 파티션 수, 복제본 수, 컨슈머 그룹 전략 등을 트래픽에 맞게 조정해야 하지만, 현재 단계의 목표는 "Producer-Consumer 구조 자체를 이해하고 적용하는 것"이므로 최소 구성으로 진행한다

---

## 7. 다중 인스턴스 환경에서의 고려사항

- 재고/잔액/쿠폰 수량 같은 **공유 상태는 DB 락으로 일관성을 보장**하므로, 인스턴스 수와 무관하게 정합성이 유지된다
- 인기 상품 조회는 각 인스턴스가 동일한 DB를 조회하므로 별도 동기화 없이 동일한 결과를 반환한다
- 세션이나 인증 토큰(JWT)을 사용해 인스턴스 간 상태 공유가 필요 없는 stateless 구조를 유지한다

---

## 8. 추후 확장 고려 사항 (커리큘럼 연계)

| 단계 | 내용 | 현재 구조와의 연결점 |
|------|------|----------------------|
| STEP05~06 | 동시성 제어 / 분산락 | 현재 비관적 락(DB 락)을 Redis 분산락(`Redisson`)과 비교, 트래픽 규모에 따른 트레이드오프 분석 |
| STEP06~07 | Redis 기반 캐싱 | 인기 상품 통계를 Redis Sorted Set / 캐시로 전환 |
| STEP08 | 이벤트 기반 아키텍처 | 현재 `ApplicationEventPublisher` + Kafka 구조를 핵심/부가 로직 분리(EDA) 관점으로 재정리 |
| STEP09 | Kafka 대규모 확장 | 현재 단일 브로커/단일 파티션 구성을 멀티 파티션, 컨슈머 그룹 분산 처리로 확장 |
| STEP10 | 장애대응 프로세스 | 부하 테스트, 모니터링, 장애 시나리오 대응 절차 수립 |

현재 단계(STEP02)의 목표는 위 항목들을 "당장 구현"하는 것이 아니라, **나중에 자연스럽게 확장할 수 있는 구조(도메인 분리, 트랜잭션 경계, 이벤트 발행 지점)를 먼저 만들어두는 것**이다.
