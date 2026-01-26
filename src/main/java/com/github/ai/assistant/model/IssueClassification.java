package com.github.ai.assistant.model;

import java.util.List;

/**
 * Issue 分类结果 - 使用 Java 21 Record
 */
public record IssueClassification(
    String type,           // bug, feature, question, documentation, etc.
    String priority,       // low, medium, high, critical
    List<String> suggestedLabels,
    String reasoning
) {
    /**
     * Issue 类型枚举
     */
    public enum IssueType {
        BUG("bug"),
        FEATURE("feature"),
        ENHANCEMENT("enhancement"),
        QUESTION("question"),
        DOCUMENTATION("documentation"),
        OTHER("other");

        private final String value;

        IssueType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * 优先级枚举
     */
    public enum Priority {
        LOW("low"),
        MEDIUM("medium"),
        HIGH("high"),
        CRITICAL("critical");

        private final String value;

        Priority(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
