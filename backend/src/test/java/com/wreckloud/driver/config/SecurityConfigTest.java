package com.wreckloud.driver.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 管理员账号角色配置测试。
 *
 * @author Wreckloud
 * @since 2026-08-21
 */
class SecurityConfigTest {

    @Test
    void shouldCreateAdministratorAndReadOnlyViewer() {
        AppProperties properties = new AppProperties(
                new AppProperties.Admin("admin", "$2a$12$" + "a".repeat(53)),
                new AppProperties.Viewer("HYHTLLWLYXGS", "$2a$12$" + "b".repeat(53)),
                new AppProperties.Photo("./runtime/uploads", 9, 2097152, 2048, 2048),
                new AppProperties.TencentMap("", "https://apis.map.qq.com/ws/geocoder/v1/"));

        var users = new SecurityConfig().userDetailsService(properties);

        assertThat(users.loadUserByUsername("admin").getAuthorities())
                .extracting("authority").containsExactly("ROLE_ADMIN");
        assertThat(users.loadUserByUsername("HYHTLLWLYXGS").getAuthorities())
                .extracting("authority").containsExactly("ROLE_VIEWER");
    }
}
