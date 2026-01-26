package com.github.ai.assistant.service;

import com.github.ai.assistant.client.GitHubClientService;
import com.github.ai.assistant.model.IssueClassification;
import org.kohsuke.github.GHIssue;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Issue 管理服务
 * 
 * 使用 AI 进行 Issue 分类、回复建议等
 */
@Service
public class IssueService {

    private final AIService aiService;
    private final GitHubClientService githubClient;

    public IssueService(AIService aiService, GitHubClientService githubClient) {
        this.aiService = aiService;
        this.githubClient = githubClient;
    }

    /**
     * 分类 Issue
     */
    public IssueClassification classifyIssue(String repository, int issueNumber, String model) {
        try {
            GHIssue issue = githubClient.getIssue(repository, issueNumber);
            
            String systemPrompt = """
                你是一个项目管理专家，擅长对 GitHub Issue 进行分类。
                
                请分析 Issue 并输出：
                1. 类型: bug/feature/enhancement/question/documentation/other
                2. 优先级: low/medium/high/critical
                3. 建议标签: 最多3个相关标签
                4. 分类理由: 简短说明
                
                格式：
                类型: xxx
                优先级: xxx
                标签: xxx, xxx
                理由: xxx
                """;
            
            String userMessage = """
                请分类以下 Issue：
                
                标题: %s
                
                内容:
                %s
                """.formatted(issue.getTitle(), issue.getBody());
            
            String response = aiService.chat(model, systemPrompt, userMessage);
            
            return parseClassificationResponse(response);
        } catch (IOException e) {
            throw new RuntimeException("获取 Issue 失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成回复建议
     */
    public String suggestReply(String repository, int issueNumber, String model) {
        try {
            GHIssue issue = githubClient.getIssue(repository, issueNumber);
            
            String systemPrompt = """
                你是一个友善、专业的开源项目维护者。
                请为这个 Issue 生成一个合适的回复。
                
                回复要求：
                1. 友善礼貌
                2. 专业准确
                3. 如果是 bug，确认问题并说明下一步
                4. 如果是 feature，表示感谢并说明考虑情况
                5. 如果需要更多信息，礼貌地询问
                
                只输出回复内容，不要其他说明。
                """;
            
            String userMessage = """
                Issue 标题: %s
                
                Issue 内容:
                %s
                
                请生成回复：
                """.formatted(issue.getTitle(), issue.getBody());
            
            return aiService.chat(model, systemPrompt, userMessage);
        } catch (IOException e) {
            throw new RuntimeException("获取 Issue 失败: " + e.getMessage(), e);
        }
    }

    /**
     * 汇总所有 Open Issues
     */
    public String summarizeOpenIssues(String repository, String model) {
        try {
            List<GHIssue> issues = githubClient.getOpenIssues(repository);
            
            if (issues.isEmpty()) {
                return "🎉 没有 Open 的 Issues！";
            }
            
            String issueList = issues.stream()
                .limit(20)  // 限制数量
                .map(issue -> "- #%d: %s".formatted(issue.getNumber(), issue.getTitle()))
                .collect(Collectors.joining("\n"));
            
            String systemPrompt = """
                你是一个项目管理专家。
                请汇总以下 Issues，输出：
                1. 总体情况概述
                2. 按类型分类统计
                3. 建议优先处理的 Issue（如果能判断）
                4. 总结和建议
                
                用中文输出，格式清晰。
                """;
            
            String userMessage = """
                仓库: %s
                Open Issues 数量: %d
                
                Issues 列表:
                %s
                
                请汇总分析：
                """.formatted(repository, issues.size(), issueList);
            
            return aiService.chat(model, systemPrompt, userMessage);
        } catch (IOException e) {
            throw new RuntimeException("获取 Issues 失败: " + e.getMessage(), e);
        }
    }

    private IssueClassification parseClassificationResponse(String response) {
        String type = extractValue(response, "类型");
        String priority = extractValue(response, "优先级");
        String labelsStr = extractValue(response, "标签");
        String reasoning = extractValue(response, "理由");
        
        List<String> labels = Arrays.stream(labelsStr.split("[,，]"))
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .toList();
        
        return new IssueClassification(type, priority, labels, reasoning);
    }

    private String extractValue(String text, String key) {
        for (String line : text.split("\n")) {
            if (line.contains(key + ":") || line.contains(key + "：")) {
                int idx = line.indexOf(":");
                if (idx == -1) idx = line.indexOf("：");
                if (idx != -1 && idx < line.length() - 1) {
                    return line.substring(idx + 1).trim();
                }
            }
        }
        return "";
    }
}
