package com.github.ai.assistant.service;

import com.github.ai.assistant.client.GitHubClientService;
import com.github.ai.assistant.model.PullRequestInfo;
import com.github.ai.assistant.model.ReviewResult;
import org.kohsuke.github.GHPullRequestReviewEvent;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * PR 审查服务
 * 
 * 使用 AI 分析 Pull Request 代码变更
 */
@Service
public class ReviewService {

    private final AIService aiService;
    private final GitHubClientService githubClient;

    public ReviewService(AIService aiService, GitHubClientService githubClient) {
        this.aiService = aiService;
        this.githubClient = githubClient;
    }

    /**
     * 审查 Pull Request
     */
    public ReviewResult reviewPullRequest(String repository, int prNumber, String focus, String model) 
            throws IOException {
        
        // 获取 PR 信息
        PullRequestInfo pr = githubClient.getPullRequest(repository, prNumber);
        
        String systemPrompt = buildSystemPrompt(focus);
        String userMessage = buildUserMessage(pr);
        
        // 调用 AI 进行审查
        String response = aiService.chat(model, systemPrompt, userMessage);
        
        // 解析 AI 响应
        return parseReviewResponse(response);
    }

    /**
     * 发布审查评论到 GitHub
     */
    public void postReviewComment(String repository, int prNumber, ReviewResult result) throws IOException {
        String body = formatReviewAsMarkdown(result);
        
        GHPullRequestReviewEvent event = result.score() >= 80 
            ? GHPullRequestReviewEvent.APPROVE 
            : GHPullRequestReviewEvent.COMMENT;
        
        githubClient.createPullRequestReview(repository, prNumber, body, event);
    }

    private String buildSystemPrompt(String focus) {
        String focusInstruction = switch (focus.toLowerCase()) {
            case "security" -> "重点关注安全问题：SQL注入、XSS、敏感信息泄露等";
            case "performance" -> "重点关注性能问题：N+1查询、内存泄漏、算法复杂度等";
            case "style" -> "重点关注代码风格：命名规范、代码结构、注释等";
            default -> "全面审查：安全、性能、代码风格、最佳实践";
        };

        return """
            你是一个资深的代码审查专家，擅长发现代码中的问题和改进点。
            
            审查重点：%s
            
            请按以下格式输出审查结果：
            
            ## 总结
            [一句话总结这个 PR]
            
            ## 评分
            [0-100 分数]
            
            ## 问题
            - [问题1]
            - [问题2]
            
            ## 建议
            - [建议1]
            - [建议2]
            
            注意：
            1. 客观公正，有理有据
            2. 问题要具体到文件和行号（如果可能）
            3. 建议要可操作
            """.formatted(focusInstruction);
    }

    private String buildUserMessage(PullRequestInfo pr) {
        return """
            请审查以下 Pull Request：
            
            ## PR 信息
            - 标题: %s
            - 作者: %s
            - 分支: %s -> %s
            - 变更文件数: %d
            - 总变更行数: %d
            
            ## PR 描述
            %s
            
            ## 代码变更
            ```diff
            %s
            ```
            """.formatted(
                pr.title(),
                pr.author(),
                pr.headRef(),
                pr.baseRef(),
                pr.files().size(),
                pr.totalChanges(),
                pr.body() != null ? pr.body() : "(无描述)",
                truncateDiff(pr.diff(), 6000)
            );
    }

    private ReviewResult parseReviewResponse(String response) {
        // 简单解析 AI 响应
        String summary = extractSection(response, "## 总结", "## 评分");
        int score = extractScore(response);
        List<String> issues = extractList(response, "## 问题", "## 建议");
        List<String> suggestions = extractList(response, "## 建议", null);
        
        return new ReviewResult(summary, score, issues, suggestions, List.of());
    }

    private String extractSection(String text, String start, String end) {
        int startIdx = text.indexOf(start);
        if (startIdx == -1) return "";
        startIdx += start.length();
        
        int endIdx = end != null ? text.indexOf(end) : text.length();
        if (endIdx == -1) endIdx = text.length();
        
        return text.substring(startIdx, endIdx).trim();
    }

    private int extractScore(String text) {
        try {
            String scoreSection = extractSection(text, "## 评分", "## 问题");
            // 只提取第一个数字序列，避免 "85/100" 变成 "85100"
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\d+").matcher(scoreSection);
            if (matcher.find()) {
                int score = Integer.parseInt(matcher.group());
                return Math.min(score, 100); // 确保不超过 100
            }
            return 70;
        } catch (Exception e) {
            return 70; // 默认分数
        }
    }

    private List<String> extractList(String text, String start, String end) {
        String section = extractSection(text, start, end);
        return Arrays.stream(section.split("\n"))
            .map(String::trim)
            .filter(line -> line.startsWith("-"))
            .map(line -> line.substring(1).trim())
            .filter(line -> !line.isBlank())
            .toList();
    }

    private String formatReviewAsMarkdown(ReviewResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 🤖 AI Code Review\n\n");
        sb.append("**评分**: ").append(result.score()).append("/100\n\n");
        sb.append("### 总结\n").append(result.summary()).append("\n\n");
        
        if (!result.issues().isEmpty()) {
            sb.append("### ⚠️ 问题\n");
            result.issues().forEach(issue -> sb.append("- ").append(issue).append("\n"));
            sb.append("\n");
        }
        
        if (!result.suggestions().isEmpty()) {
            sb.append("### 💡 建议\n");
            result.suggestions().forEach(s -> sb.append("- ").append(s).append("\n"));
        }
        
        sb.append("\n---\n*Generated by GitHub AI Assistant*");
        return sb.toString();
    }

    private String truncateDiff(String diff, int maxLength) {
        if (diff == null) return "";
        if (diff.length() <= maxLength) return diff;
        return diff.substring(0, maxLength) + "\n... (truncated)";
    }
}
