package com.wreckloud.driver.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.wreckloud.driver.config.AppProperties;
import com.wreckloud.driver.service.ReverseGeocodingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;

/**
 * 腾讯位置服务逆地址解析实现。
 *
 * @author Wreckloud
 * @since 2026-07-27
 */
@Slf4j
@Service
public class TencentReverseGeocodingService implements ReverseGeocodingService {
    private final AppProperties properties;
    private final RestClient restClient;

    public TencentReverseGeocodingService(AppProperties properties) {
        this.properties = properties;
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(3));
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    @Override
    public String resolveAddress(BigDecimal latitude, BigDecimal longitude) {
        if (!StringUtils.hasText(properties.tencentMap().key())) {
            log.warn("Tencent map key is not configured; address resolution skipped");
            return null;
        }
        try {
            JsonNode response = restClient.get()
                    .uri(properties.tencentMap().endpoint(), uriBuilder -> uriBuilder
                            .queryParam("location", latitude.toPlainString() + "," + longitude.toPlainString())
                            .queryParam("key", properties.tencentMap().key())
                            .queryParam("coord_type", 1)
                            .queryParam("get_poi", 0)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
            if (response != null && response.path("status").asInt(-1) == 0) {
                String address = response.path("result").path("address").asText(null);
                return StringUtils.hasText(address) ? address : null;
            }
            log.warn("Tencent reverse geocoding returned a business error, status={}",
                    response == null ? "empty" : response.path("status").asText());
        } catch (Exception exception) {
            log.warn("Tencent reverse geocoding failed: {}", exception.getClass().getSimpleName());
        }
        return null;
    }
}
