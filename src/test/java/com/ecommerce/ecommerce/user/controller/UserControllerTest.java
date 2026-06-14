package com.ecommerce.ecommerce.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecommerce.ecommerce.common.exception.BusinessException;
import com.ecommerce.ecommerce.common.exception.ErrorCode;
import com.ecommerce.ecommerce.global.config.JpaAuditingConfig;
import com.ecommerce.ecommerce.user.dto.UserResponse;
import com.ecommerce.ecommerce.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = UserController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    @DisplayName("GET /api/v1/users - 200 OK, 유저 목록 반환")
    void findAll_정상조회_200OK() throws Exception {
        UserResponse response1 = new UserResponse(1L, "a@example.com", "유저A", LocalDateTime.now(), LocalDateTime.now());
        UserResponse response2 = new UserResponse(2L, "b@example.com", "유저B", LocalDateTime.now(), LocalDateTime.now());
        given(userService.findAll()).willReturn(List.of(response1, response2));

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].email").value("a@example.com"))
                .andExpect(jsonPath("$[1].email").value("b@example.com"));
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} - 200 OK, 유저 반환")
    void findById_정상조회_200OK() throws Exception {
        UserResponse response = new UserResponse(1L, "test@example.com", "테스터", LocalDateTime.now(), LocalDateTime.now());
        given(userService.findById(1L)).willReturn(response);

        mockMvc.perform(get("/api/v1/users/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.name").value("테스터"));
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} - 존재하지 않는 유저 -> 404 Not Found")
    void findById_존재하지않는유저_404NotFound() throws Exception {
        given(userService.findById(1L)).willThrow(new BusinessException(ErrorCode.NOT_FOUND, "User not found. id=1"));

        mockMvc.perform(get("/api/v1/users/{id}", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("POST /api/v1/users - 201 Created, 유저 생성")
    void create_정상요청_201Created() throws Exception {
        String requestBody = """
                {
                  "email": "test@example.com",
                  "password": "password123",
                  "name": "테스터"
                }
                """;
        UserResponse response = new UserResponse(1L, "test@example.com", "테스터", LocalDateTime.now(), LocalDateTime.now());
        given(userService.create(any())).willReturn(response);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    @DisplayName("POST /api/v1/users - 필수 필드 누락 -> 400 Bad Request")
    void create_유효성검증실패_400BadRequest() throws Exception {
        String requestBody = """
                {
                  "email": "not-an-email",
                  "password": "",
                  "name": ""
                }
                """;

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/users - 이메일 중복 -> 409 Conflict")
    void create_이메일중복_409Conflict() throws Exception {
        String requestBody = """
                {
                  "email": "test@example.com",
                  "password": "password123",
                  "name": "테스터"
                }
                """;
        given(userService.create(any())).willThrow(new BusinessException(ErrorCode.DUPLICATE_EMAIL));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"));
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id} - 200 OK, 유저 정보 수정")
    void update_정상요청_200OK() throws Exception {
        String requestBody = """
                {
                  "email": "new@example.com",
                  "password": "password123",
                  "name": "새이름"
                }
                """;
        UserResponse response = new UserResponse(1L, "new@example.com", "새이름", LocalDateTime.now(), LocalDateTime.now());
        given(userService.update(eq(1L), any())).willReturn(response);

        mockMvc.perform(put("/api/v1/users/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new@example.com"))
                .andExpect(jsonPath("$.name").value("새이름"));
    }

    @Test
    @DisplayName("DELETE /api/v1/users/{id} - 204 No Content")
    void delete_정상요청_204NoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/users/{id}", 1L))
                .andExpect(status().isNoContent());
    }

    // TODO: PATCH /api/v1/users/{userId}/balance, GET /api/v1/users/{userId}/balance - 미구현
    // 구현 후 200 OK / amount<=0 -> 400 / 존재하지 않는 userId -> 404 테스트 추가 필요 (docs/api-spec.md 1번 참고)
}
