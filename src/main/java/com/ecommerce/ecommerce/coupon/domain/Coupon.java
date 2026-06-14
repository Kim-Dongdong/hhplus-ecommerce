package com.ecommerce.ecommerce.coupon.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "coupons")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private double discountRate;

    @Column(nullable = false)
    private int totalQuantity;

    @Column(nullable = false)
    private int issuedQuantity;

    @Column(nullable = false)
    private LocalDateTime expiredAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Coupon(String name, double discountRate, int totalQuantity, LocalDateTime expiredAt) {
        this.name = name;
        this.discountRate = discountRate;
        this.totalQuantity = totalQuantity;
        this.expiredAt = expiredAt;
        this.issuedQuantity = 0;
    }

    public void update(String name, double discountRate, int totalQuantity, LocalDateTime expiredAt) {
        this.name = name;
        this.discountRate = discountRate;
        this.totalQuantity = totalQuantity;
        this.expiredAt = expiredAt;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiredAt);
    }

    public boolean isSoldOut() {
        return this.issuedQuantity >= this.totalQuantity;
    }

    public void increaseIssuedQuantity() {
        this.issuedQuantity++;
    }
}
