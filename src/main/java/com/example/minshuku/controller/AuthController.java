package com.example.minshuku.controller;

import com.example.minshuku.service.AdminUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** React ログイン画面から利用する管理者認証 API。 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contextRepository;
    private final AdminUserService users;

    public AuthController(AuthenticationManager authenticationManager, SecurityContextRepository contextRepository,
            AdminUserService users) {
        this.authenticationManager = authenticationManager;
        this.contextRepository = contextRepository;
        this.users = users;
    }

    @GetMapping("/status")
    public AuthStatus status(Authentication authentication) {
        boolean authenticated = authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal());
        return new AuthStatus(authenticated, users.setupRequired(), authenticated ? authentication.getName() : null);
    }

    @PostMapping("/setup")
    public Map<String, String> setup(@RequestBody Credentials request) {
        users.setup(request.username(), request.password());
        return Map.of("message", "管理者を登録しました。");
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody Credentials request, HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
            // 認証前のセッションIDを引き継がず、固定化攻撃を防ぐ。
            servletRequest.getSession(true);
            servletRequest.changeSessionId();
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            contextRepository.saveContext(context, servletRequest, servletResponse);
            users.recordLoginSuccess(authentication.getName());
            return Map.of("message", "ログインしました。");
        } catch (AuthenticationException ex) {
            users.recordLoginFailure(request.username());
            throw new IllegalArgumentException("ユーザー名またはパスワードが正しくありません。");
        }
    }

    @PostMapping("/logout")
    public Map<String, String> logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        if (request.getSession(false) != null)
            request.getSession(false).invalidate();
        return Map.of("message", "ログアウトしました。");
    }

    public record Credentials(String username, String password) {
    }
    public record AuthStatus(boolean authenticated, boolean setupRequired, String username) {
    }
}
