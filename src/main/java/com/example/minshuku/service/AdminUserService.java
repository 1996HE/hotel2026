package com.example.minshuku.service;

import com.example.minshuku.domain.AdminUser;
import com.example.minshuku.mapper.AdminUserMapper;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 管理者の初期作成、認証参照、失敗回数およびパスワード変更を扱う。 */
@Service
public class AdminUserService implements UserDetailsService {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("[A-Za-z0-9._-]{3,64}");
    private static final int MIN_PASSWORD_LENGTH = 10;
    private static final int LOCK_MINUTES = 15;

    private final AdminUserMapper mapper;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(AdminUserMapper mapper, PasswordEncoder passwordEncoder) {
        this.mapper = mapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public boolean setupRequired() {
        return mapper.countAll() == 0;
    }

    /** 最初の一人だけ作成できるため、公開セットアップAPIから複数管理者は増やせない。 */
    @Transactional
    public void setup(String username, String password) {
        if (!setupRequired()) {
            throw new IllegalArgumentException("管理者はすでに登録されています。");
        }
        validateCredentials(username, password);
        AdminUser user = new AdminUser();
        user.setUsername(normalizeUsername(username));
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setEnabled(true);
        mapper.insert(user);
    }

    @Transactional
    public void recordLoginSuccess(String username) {
        mapper.recordSuccess(normalizeUsername(username));
    }

    @Transactional
    public void recordLoginFailure(String username) {
        if (StringUtils.hasText(username)) {
            mapper.recordFailure(normalizeUsername(username), OffsetDateTime.now().plusMinutes(LOCK_MINUTES));
        }
    }

    @Transactional
    public void changePassword(String username, String newPassword) {
        validateCredentials(username, newPassword);
        if (mapper.updatePassword(normalizeUsername(username), passwordEncoder.encode(newPassword)) == 0) {
            throw new IllegalArgumentException("管理者が見つかりません。");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AdminUser user = mapper.findByUsername(normalizeUsername(username));
        if (user == null)
            throw new UsernameNotFoundException("管理者が見つかりません。");
        boolean accountNonLocked = user.getLockedUntil() == null
                || user.getLockedUntil().isBefore(OffsetDateTime.now());
        return User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .roles("ADMIN")
                .disabled(!Boolean.TRUE.equals(user.getEnabled()))
                .accountLocked(!accountNonLocked)
                .build();
    }

    private void validateCredentials(String username, String password) {
        if (!StringUtils.hasText(username) || !USERNAME_PATTERN.matcher(username.trim()).matches()) {
            throw new IllegalArgumentException("ユーザー名は3〜64文字の英数字、._-で入力してください。");
        }
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("パスワードは10文字以上で入力してください。");
        }
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}
