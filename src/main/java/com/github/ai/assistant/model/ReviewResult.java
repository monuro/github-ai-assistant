package com.github.ai.assistant.model;

import java.util.List;

/**
 * PR 审查结果 - 使用 Java 21 Record
 */
public record ReviewResult(
    String summary,
    int score,
    List<String> issues,
    List<String> suggestions,
    List<FileReview> fileReviews
) {
    /**
     * 单个文件的审查结果
     */
    public record FileReview(
        String filename,
        int score,
        List<LineComment> comments
    ) {}

    /**
     * 行级别的评论
     */
    public record LineComment(
        int line,
        String type,    // warning, suggestion, error
        String message
    ) {}

    /**
     * 创建一个空的审查结果
     */
    public static ReviewResult empty() {
        return new ReviewResult("", 0, List.of(), List.of(), List.of());
    }
}
