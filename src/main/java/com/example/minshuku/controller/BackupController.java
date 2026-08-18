package com.example.minshuku.controller;

import com.example.minshuku.domain.BackupRecord;
import com.example.minshuku.service.DatabaseBackupService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** バックアップ保存先、実行履歴、手動実行を提供する。 */
@RestController
@RequestMapping("/api/backups")
public class BackupController {
    private final DatabaseBackupService service;

    public BackupController(DatabaseBackupService service) {
        this.service = service;
    }

    @GetMapping
    public BackupStatus status() {
        return new BackupStatus(service.getDirectory(), service.findRecent());
    }

    @PostMapping
    public BackupRecord create() {
        return service.createBackup();
    }

    public record BackupStatus(String directory, List<BackupRecord> history) {
    }
}
