package com.example.minshuku.mapper;

import com.example.minshuku.domain.AdminUser;
import java.time.OffsetDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 管理者認証情報をDBに保存する MyBatis Mapper。 */
@Mapper
public interface AdminUserMapper {
    int countAll();

    AdminUser findByUsername(@Param("username") String username);

    int insert(AdminUser user);

    int recordSuccess(@Param("username") String username);

    int recordFailure(@Param("username") String username, @Param("lockedUntil") OffsetDateTime lockedUntil);

    int updatePassword(@Param("username") String username, @Param("passwordHash") String passwordHash);
}
