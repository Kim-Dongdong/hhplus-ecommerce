package com.ecommerce.ecommerce.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ecommerce.ecommerce.common.exception.BusinessException;
import com.ecommerce.ecommerce.common.exception.ErrorCode;
import com.ecommerce.ecommerce.user.domain.User;
import com.ecommerce.ecommerce.user.dto.UserRequest;
import com.ecommerce.ecommerce.user.dto.UserResponse;
import com.ecommerce.ecommerce.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("create_정상요청_유저생성")
    void create_정상요청_유저생성() {
        UserRequest request = new UserRequest("test@example.com", "password123", "테스터");
        User user = new User(request.email(), "encodedPassword", request.name());
        given(userRepository.existsByEmail(request.email())).willReturn(false);
        given(passwordEncoder.encode(request.password())).willReturn("encodedPassword");
        given(userRepository.save(any(User.class))).willReturn(user);

        UserResponse result = userService.create(request);

        assertThat(result.email()).isEqualTo(request.email());
        assertThat(result.name()).isEqualTo(request.name());
        verify(passwordEncoder).encode(request.password());
    }

    @Test
    @DisplayName("create_이메일중복_예외발생")
    void create_이메일중복_예외발생() {
        UserRequest request = new UserRequest("test@example.com", "password123", "테스터");
        given(userRepository.existsByEmail(request.email())).willReturn(true);

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_EMAIL));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("findById_정상조회_유저반환")
    void findById_정상조회_유저반환() {
        User user = new User("test@example.com", "encodedPassword", "테스터");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        UserResponse result = userService.findById(1L);

        assertThat(result.email()).isEqualTo("test@example.com");
        assertThat(result.name()).isEqualTo("테스터");
    }

    @Test
    @DisplayName("findById_존재하지않는유저_예외발생")
    void findById_존재하지않는유저_예외발생() {
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    @DisplayName("findAll_정상조회_유저목록반환")
    void findAll_정상조회_유저목록반환() {
        User user1 = new User("a@example.com", "encoded", "유저A");
        User user2 = new User("b@example.com", "encoded", "유저B");
        given(userRepository.findAll()).willReturn(List.of(user1, user2));

        List<UserResponse> result = userService.findAll();

        assertThat(result).hasSize(2)
                .extracting(UserResponse::email)
                .containsExactly("a@example.com", "b@example.com");
    }

    @Test
    @DisplayName("update_정상요청_유저정보수정")
    void update_정상요청_유저정보수정() {
        User user = new User("old@example.com", "encoded", "기존이름");
        UserRequest request = new UserRequest("new@example.com", "password123", "새이름");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.existsByEmail(request.email())).willReturn(false);

        UserResponse result = userService.update(1L, request);

        assertThat(result.email()).isEqualTo("new@example.com");
        assertThat(result.name()).isEqualTo("새이름");
    }

    @Test
    @DisplayName("update_이메일중복_예외발생")
    void update_이메일중복_예외발생() {
        User user = new User("old@example.com", "encoded", "기존이름");
        UserRequest request = new UserRequest("duplicate@example.com", "password123", "새이름");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.existsByEmail(request.email())).willReturn(true);

        assertThatThrownBy(() -> userService.update(1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_EMAIL));
    }

    @Test
    @DisplayName("delete_정상요청_유저삭제")
    void delete_정상요청_유저삭제() {
        User user = new User("test@example.com", "encoded", "테스터");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        userService.delete(1L);

        verify(userRepository).delete(user);
    }

    @Test
    @DisplayName("delete_존재하지않는유저_예외발생")
    void delete_존재하지않는유저_예외발생() {
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.delete(1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    // TODO: chargeBalance / getBalance - User에 잔액(balance) 필드 및 UserService.chargeBalance/getBalance 미구현.
    // PATCH /api/v1/users/{userId}/balance, GET /api/v1/users/{userId}/balance 구현 후 테스트 추가 필요 (docs/api-spec.md 1번 참고)
}
