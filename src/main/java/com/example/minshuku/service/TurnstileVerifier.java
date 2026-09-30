package com.example.minshuku.service;

import com.example.minshuku.config.PublicBookingProperties;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/** 正式環境で Cloudflare Turnstile の応答をサーバー側検証する。 */
@Service
public class TurnstileVerifier {
    private final PublicBookingProperties properties;
    private final RestClient restClient = RestClient.create();

    public TurnstileVerifier(PublicBookingProperties properties) {
        this.properties = properties;
    }

    public void verify(String token, String remoteIp) {
        if (!properties.isTurnstileRequired()) {
            return;
        }
        if (!StringUtils.hasText(properties.getTurnstileSecretKey())) {
            throw new IllegalStateException("Turnstile secret key is not configured.");
        }
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("防机器人验证尚未完成。");
        }
        LinkedMultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("secret", properties.getTurnstileSecretKey());
        body.add("response", token);
        if (StringUtils.hasText(remoteIp)) {
            body.add("remoteip", remoteIp);
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
                .uri("https://challenges.cloudflare.com/turnstile/v0/siteverify")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(Map.class);
        if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
            throw new IllegalArgumentException("防机器人验证失败，请重新操作。");
        }
    }
}
