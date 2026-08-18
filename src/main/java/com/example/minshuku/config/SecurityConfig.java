package com.example.minshuku.config;

import com.example.minshuku.service.AdminUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/** 管理画面の認証、CSRF、公開範囲およびブラウザ保護ヘッダーを集約する。 */
@Configuration
public class SecurityConfig {
    private static final String[] SOURCE_LIKE_PATHS = {
            "/**/*.java", "/**/*.class", "/**/*.kt", "/**/*.groovy", "/**/*.xml",
            "/**/*.yml", "/**/*.yaml", "/**/*.properties", "/**/*.sql", "/**/*.map",
            "/.git/**", "/.env", "/pom.xml", "/build.gradle", "/gradle.properties"
    };
    private static final String CONTENT_SECURITY_POLICY = String.join("; ",
            "default-src 'self'", "script-src 'self'", "style-src 'self'", "img-src 'self' data:",
            "font-src 'self'", "connect-src 'self'", "form-action 'self'", "frame-ancestors 'none'", "base-uri 'self'");

    private static RequestMatcher[] sourceLikeMatchers() {
        RequestMatcher[] matchers = new RequestMatcher[SOURCE_LIKE_PATHS.length];
        for (int i = 0; i < SOURCE_LIKE_PATHS.length; i += 1) {
            matchers[i] = AntPathRequestMatcher.antMatcher(SOURCE_LIKE_PATHS[i]);
        }
        return matchers;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    AuthenticationManager authenticationManager(AdminUserService users, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(users);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository repository) throws Exception {
        AuthenticationEntryPoint apiEntryPoint = new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);
        AuthenticationEntryPoint pageEntryPoint = new LoginUrlAuthenticationEntryPoint("/login");
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(sourceLikeMatchers()).denyAll()
                        .requestMatchers("/login", "/setup", "/api/auth/**", "/error", "/js/**", "/styles/**",
                                "/actuator/health/**")
                        .permitAll()
                        .anyRequest().authenticated())
                .securityContext(context -> context.securityContextRepository(repository))
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(apiEntryPoint, new AntPathRequestMatcher("/api/**"))
                        .defaultAuthenticationEntryPointFor(pageEntryPoint, new AntPathRequestMatcher("/**")))
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
