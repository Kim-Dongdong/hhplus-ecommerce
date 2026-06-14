package com.ecommerce.ecommerce.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    OUT_OF_STOCK(HttpStatus.CONFLICT, "재고가 부족합니다."),
    COUPON_OUT_OF_STOCK(HttpStatus.CONFLICT, "쿠폰 수량이 모두 소진되었습니다."),
    COUPON_EXPIRED(HttpStatus.BAD_REQUEST, "쿠폰 유효기간이 만료되었습니다."),
    COUPON_ALREADY_ISSUED(HttpStatus.CONFLICT, "이미 발급받은 쿠폰입니다."),
    INSUFFICIENT_BALANCE(HttpStatus.PAYMENT_REQUIRED, "잔액이 부족합니다."),
    INVALID_COUPON(HttpStatus.BAD_REQUEST, "유효하지 않은 쿠폰입니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
