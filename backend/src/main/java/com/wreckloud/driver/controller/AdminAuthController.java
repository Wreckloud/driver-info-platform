package com.wreckloud.driver.controller;

import com.wreckloud.driver.common.ApiResult;
import com.wreckloud.driver.dto.AdminLoginRequest;
import com.wreckloud.driver.exception.BusinessException;
import com.wreckloud.driver.security.LoginAttemptService;
import com.wreckloud.driver.vo.AdminSessionVO;
import com.wreckloud.driver.vo.CsrfTokenVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员认证接口。
 *
 * @author Wreckloud
 * @since 2026-07-27
 */
@Tag(name = "管理员认证")
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {
    private final AuthenticationManager authenticationManager;
    private final LoginAttemptService loginAttemptService;
    private final HttpSessionSecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    @Operation(summary = "获取 CSRF 令牌")
    @GetMapping("/csrf")
    public ApiResult<CsrfTokenVO> csrf(CsrfToken csrfToken) {
        return ApiResult.success(new CsrfTokenVO(
                csrfToken.getToken(), csrfToken.getHeaderName(), csrfToken.getParameterName()));
    }

    @Operation(summary = "管理员登录")
    @PostMapping("/login")
    public ApiResult<AdminSessionVO> login(@Valid @RequestBody AdminLoginRequest loginRequest,
                                           HttpServletRequest request,
                                           HttpServletResponse response) {
        String clientAddress = request.getRemoteAddr();
        if (loginAttemptService.isBlocked(clientAddress)) {
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "登录失败次数过多，请 15 分钟后重试");
        }
        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            loginRequest.username().trim(), loginRequest.password()));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            if (request.getSession(false) != null) {
                request.changeSessionId();
            }
            securityContextRepository.saveContext(context, request, response);
            loginAttemptService.recordSuccess(clientAddress);
            return ApiResult.success(new AdminSessionVO(authentication.getName()));
        } catch (BadCredentialsException exception) {
            loginAttemptService.recordFailure(clientAddress);
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }
    }

    @Operation(summary = "管理员退出")
    @PostMapping("/logout")
    public ApiResult<Void> logout(Authentication authentication,
                                  HttpServletRequest request,
                                  HttpServletResponse response) {
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        return ApiResult.success();
    }

    @Operation(summary = "获取当前管理员")
    @GetMapping("/me")
    public ApiResult<AdminSessionVO> me(Authentication authentication) {
        return ApiResult.success(new AdminSessionVO(authentication.getName()));
    }
}
