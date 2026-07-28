package com.wreckloud.driver.tool;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 管理员 BCrypt 密码摘要生成工具。
 *
 * @author Wreckloud
 * @since 2026-07-27
 */
public final class PasswordHashTool {

    private PasswordHashTool() {
    }

    public static void main(String[] args) {
        String password = System.getenv("ADMIN_PASSWORD_PLAIN");
        if (password == null || password.length() < 12) {
            throw new IllegalArgumentException("ADMIN_PASSWORD_PLAIN must contain at least 12 characters");
        }
        System.out.println(new BCryptPasswordEncoder(12).encode(password));
    }
}
