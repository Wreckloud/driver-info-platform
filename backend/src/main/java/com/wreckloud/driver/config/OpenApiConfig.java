package com.wreckloud.driver.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 文档配置。
 *
 * @author Wreckloud
 * @since 2026-07-27
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI driverInfoOpenApi() {
        return new OpenAPI().info(new Info()
                .title("司机出车登记管理系统 API")
                .version("1.2.0")
                .description("司机登记与管理员后台接口"));
    }
}
