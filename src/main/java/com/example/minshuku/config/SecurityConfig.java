package com.example.minshuku.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * 管理画面のセキュリティヘッダーおよび CSRF 設定を集約する設定クラス。
 * <p>
 * 社内向け画面として、ソース公開につながるパスを遮断し、ブラウザ側の誤利用を抑止する。
 */
@Configuration
public class SecurityConfig {
    private static final String[] SOURCE_LIKE_PATHS = {
            // ソースや設定ファイルを推測されやすい拡張子・場所をまとめて拒否する。
            "/**/*.java",
            "/**/*.class",
            "/**/*.kt",
            "/**/*.groovy",
            "/**/*.xml",
            "/**/*.yml",
            "/**/*.yaml",
            "/**/*.properties",
            "/**/*.sql",
            "/**/*.map",
            "/.git/**",
            "/.env",
            "/pom.xml",
            "/build.gradle",
            "/gradle.properties"
    };

    private static final String[] PUBLIC_PAGE_PATHS = {
            // 社内画面として実際に公開するルートだけを許可する。
            "/",
            "/dashboard",
            "/rooms",
            "/rooms/**",
            "/reservations",
            "/reservations/**",
            "/prices",
            "/prices/**",
            "/api/**",
            "/error",
            "/js/**",
            "/styles/**", "/actuator/health/**"
    };

    private static final String CONTENT_SECURITY_POLICY = String.join("; ",
            // 外部スクリプトや外部送信を抑止し、内部画面の自己完結性を高める。
            "default-src 'self'",
            "script-src 'self'",
            "style-src 'self'",
            "img-src 'self' data:",
            "font-src 'self'",
            "connect-src 'self'",
            "form-action 'self'",
            "frame-ancestors 'none'",
            "base-uri 'self'");

    private static RequestMatcher[] sourceLikeMatchers() {
        // 配列定義を RequestMatcher に変換して denyAll へ渡す。
        RequestMatcher[] matchers = new RequestMatcher[SOURCE_LIKE_PATHS.length];
        for (int i = 0; i < SOURCE_LIKE_PATHS.length; i += 1) {
            matchers[i] = AntPathRequestMatcher.antMatcher(SOURCE_LIKE_PATHS[i]);
        }
        return matchers;
    }

    /**
     * 社内運用画面向けに認証画面を無効化し、基本的なブラウザ保護ヘッダーを付与する。
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(sourceLikeMatchers()).denyAll()
                        .requestMatchers(PUBLIC_PAGE_PATHS).permitAll()
                        .anyRequest().denyAll())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY))
                        .referrerPolicy(
                                referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .cacheControl(Customizer.withDefaults())
                        .addHeaderWriter(new StaticHeadersWriter("X-Robots-Tag", "noindex, nofollow, noarchive"))
                        .addHeaderWriter(new StaticHeadersWriter("X-Permitted-Cross-Domain-Policies", "none"))
                        .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy",
                                "camera=(), microphone=(), geolocation=(), payment=(), usb=()"))
                        .frameOptions(Customizer.withDefaults())
                        .contentTypeOptions(Customizer.withDefaults()));
        return http.build();
    }
}
