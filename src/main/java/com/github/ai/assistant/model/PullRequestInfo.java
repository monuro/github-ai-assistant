package com.github.ai.assistant.model;

import java.time.Instant;
import java.util.List;

/**
 * Pull Request 信息 - 使用 Java 21 Record
 */
public record PullRequestInfo(
    int number,
    String title,
    String body,
    String author,
    String state,
    String baseRef,
    String headRef,
    Instant createdAt,
    List<FileChange> files,
    String diff
) {
    /**
     * 文件变更信息
     */
    public record FileChange(
        String filename,
        String status,    // added, modified, removed, renamed
        int additions,
        int deletions,
        String patch      // diff patch
    ) {}

    /**
     * 计算总变更行数
     */
    public int totalChanges() {
        return files.stream()
            .mapToInt(f -> f.additions() + f.deletions())
            .sum();
    }
}
