package com.example.minshuku.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** 公開予約の保留、通知、Turnstile 設定。秘密値は環境変数から受け取る。 */
@Component
@ConfigurationProperties(prefix = "app.public-booking")
public class PublicBookingProperties {
    private int holdHours = 24;
    private int cancellationCutoffHours = 72;
    private String publicBaseUrl = "http://localhost:8000/jukai-internal";
    private boolean mailEnabled;
    private String mailFrom = "";
    private String adminMail = "";
    private boolean turnstileRequired;
    private String turnstileSiteKey = "";
    private String turnstileSecretKey = "";

    public int getHoldHours() {
        return holdHours;
    }

    public void setHoldHours(int holdHours) {
        this.holdHours = holdHours;
    }

    public int getCancellationCutoffHours() {
        return cancellationCutoffHours;
    }

    public void setCancellationCutoffHours(int cancellationCutoffHours) {
        this.cancellationCutoffHours = cancellationCutoffHours;
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public boolean isMailEnabled() {
        return mailEnabled;
    }

    public void setMailEnabled(boolean mailEnabled) {
        this.mailEnabled = mailEnabled;
    }

    public String getMailFrom() {
        return mailFrom;
    }

    public void setMailFrom(String mailFrom) {
        this.mailFrom = mailFrom;
    }

    public String getAdminMail() {
        return adminMail;
    }

    public void setAdminMail(String adminMail) {
        this.adminMail = adminMail;
    }

    public boolean isTurnstileRequired() {
        return turnstileRequired;
    }

    public void setTurnstileRequired(boolean turnstileRequired) {
        this.turnstileRequired = turnstileRequired;
    }

    public String getTurnstileSiteKey() {
        return turnstileSiteKey;
    }

    public void setTurnstileSiteKey(String turnstileSiteKey) {
        this.turnstileSiteKey = turnstileSiteKey;
    }

    public String getTurnstileSecretKey() {
        return turnstileSecretKey;
    }

    public void setTurnstileSecretKey(String turnstileSecretKey) {
        this.turnstileSecretKey = turnstileSecretKey;
    }

    @PostConstruct
    void validate() {
        if (holdHours < 1 || cancellationCutoffHours < 1) {
            throw new IllegalStateException("Public booking hold and cancellation cutoff must be positive.");
        }
        if (!StringUtils.hasText(publicBaseUrl)) {
            throw new IllegalStateException("PUBLIC_BASE_URL is required.");
        }
        if (turnstileRequired
                && (!StringUtils.hasText(turnstileSiteKey) || !StringUtils.hasText(turnstileSecretKey))) {
            throw new IllegalStateException(
                    "Turnstile site and secret keys are required when verification is enabled.");
        }
    }
}
