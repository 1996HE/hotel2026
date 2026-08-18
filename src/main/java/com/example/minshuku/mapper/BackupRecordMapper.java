package com.example.minshuku.mapper;

import com.example.minshuku.domain.BackupRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** バックアップ実行状態の記録と参照を行う。 */
@Mapper
public interface BackupRecordMapper {
    int insert(BackupRecord record);

    int complete(@Param("id") Integer id, @Param("status") String status,
            @Param("fileSizeBytes") Long fileSizeBytes, @Param("message") String message);

    List<BackupRecord> findRecent(@Param("limit") int limit);
}
