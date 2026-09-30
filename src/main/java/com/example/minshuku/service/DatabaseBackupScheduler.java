package com.example.minshuku.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 日次バックアップの実行窓口。保存期限による自動削除は行わない。 */
@Component
@ConditionalOnProperty(name = "app.backup.enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseBackupScheduler {
    private final DatabaseBackupService service;

    public DatabaseBackupScheduler(DatabaseBackupService service) {
        this.service = service;
    }

    @Scheduled(cron = "${app.backup.cron:0 0 2 * * *}", zone = "${app.time-zone:Asia/Tokyo}")
    public void backup() {
        service.createBackup();
    }
}
