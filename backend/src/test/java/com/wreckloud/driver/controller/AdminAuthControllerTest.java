package com.wreckloud.driver.controller;

import com.wreckloud.driver.dto.AdminLoginRequest;
import com.wreckloud.driver.security.LoginAttemptService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 管理员认证接口测试。
 *
 * @author Wreckloud
 * @since 2026-07-27
 */
class AdminAuthControllerTest {

    @Test
    void shouldChangeSessionIdAfterSuccessfulLogin() {
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        LoginAttemptService loginAttemptService = mock(LoginAttemptService.class);
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                "admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        when(authenticationManager.authenticate(any())).thenReturn(authentication);

        AdminAuthController controller = new AdminAuthController(authenticationManager, loginAttemptService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        String originalSessionId = request.getSession().getId();

        var result = controller.login(new AdminLoginRequest("admin", "LocalTestPassword123!"), request, response);

        assertThat(result.data().username()).isEqualTo("admin");
        assertThat(request.getSession().getId()).isNotEqualTo(originalSessionId);
        verify(loginAttemptService).recordSuccess(request.getRemoteAddr());
    }
}
