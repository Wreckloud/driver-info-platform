package com.wreckloud.driver;

import com.wreckloud.driver.config.AppProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 司机出车登记管理系统启动类。
 *
 * @author Wreckloud
 * @since 2026-07-27
 */
@MapperScan("com.wreckloud.driver.mapper")
@EnableConfigurationProperties(AppProperties.class)
@SpringBootApplication
public class DriverInfoPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(DriverInfoPlatformApplication.class, args);
    }
}
