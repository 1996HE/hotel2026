package com.example.minshuku.service;

import com.example.minshuku.domain.BackupRecord;
import com.example.minshuku.mapper.BackupRecordMapper;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** pg_dump を使って指定フォルダに日次バックアップを残す。 */
@Service
public class DatabaseBackupService {
    private static final Duration TIMEOUT = Duration.ofMinutes(10);
    private static final int MESSAGE_LIMIT = 900;
    private final BackupRecordMapper mapper;
    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final String command;
    private final Path directory;

    public DatabaseBackupService(BackupRecordMapper mapper,
            @Value("${spring.datasource.url}") String jdbcUrl,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password:}") String password,
            @Value("${app.backup.pg-dump-command:pg_dump}") String command,
            @Value("${app.backup.directory:./backups}") String directory) {
        this.mapper = mapper;
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.command = command;
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }

    public synchronized BackupRecord createBackup() {
        Path file = directory.resolve("minshuku-"
                + OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".dump");
        BackupRecord record = new BackupRecord();
        record.setBackupFile(file.toString());
        record.setStatus("started");
        record.setStartedAt(OffsetDateTime.now());
        mapper.insert(record);

        try {
            Files.createDirectories(directory);
            DatabaseLocation database = parseDatabaseLocation(jdbcUrl);
            ProcessBuilder builder = new ProcessBuilder(command, "--format=custom", "--no-owner", "--no-acl",
                    "--host=" + database.host(), "--port=" + database.port(), "--username=" + username,
                    "--file=" + file, database.database());
            // パスワードはコマンド行に出さず、pg_dump 専用環境変数で渡す。
            builder.environment().put("PGPASSWORD", password);
            builder.redirectErrorStream(true);
            Process process = builder.start();
            boolean completed = process.waitFor(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                throw new IllegalStateException("バックアップが10分以内に完了しませんでした。");
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (process.exitValue() != 0) {
                throw new IllegalStateException(output.isBlank() ? "pg_dump が失敗しました。" : output);
            }
            mapper.complete(record.getId(), "success", Files.size(file), "バックアップが完了しました。");
        } catch (IOException ex) {
            completeFailure(record, "pg_dump または保存先フォルダを確認してください。 " + ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            completeFailure(record, "バックアップが中断されました。");
        } catch (RuntimeException ex) {
            completeFailure(record, ex.getMessage());
        }
        return mapper.findRecent(1).get(0);
    }

    public List<BackupRecord> findRecent() {
        return mapper.findRecent(30);
    }

    public String getDirectory() {
        return directory.toString();
    }

    private void completeFailure(BackupRecord record, String message) {
        String safeMessage = message == null ? "不明なエラー" : message;
        if (safeMessage.length() > MESSAGE_LIMIT)
            safeMessage = safeMessage.substring(0, MESSAGE_LIMIT);
        mapper.complete(record.getId(), "failed", null, safeMessage);
    }

    private DatabaseLocation parseDatabaseLocation(String url) {
        if (url == null || !url.startsWith("jdbc:postgresql://")) {
            throw new IllegalStateException("PostgreSQL の JDBC URL を確認してください。");
        }
        URI uri = URI.create(url.substring("jdbc:".length()));
        String path = uri.getPath();
        if (uri.getHost() == null || path == null || path.length() <= 1) {
            throw new IllegalStateException("PostgreSQL の接続先を解析できません。");
        }
        return new DatabaseLocation(uri.getHost(), uri.getPort() > 0 ? uri.getPort() : 5432, path.substring(1));
    }

    private record DatabaseLocation(String host, int port, String database) {
    }
}
