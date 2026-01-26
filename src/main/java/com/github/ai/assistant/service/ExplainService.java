package com.github.ai.assistant.service;

import org.springframework.stereotype.Service;

/**
 * 代码/命令解释服务
 * 
 * 使用 AI 解释 Git 命令或代码片段
 */
@Service
public class ExplainService {

    private final AIService aiService;

    public ExplainService(AIService aiService) {
        this.aiService = aiService;
    }

    /**
     * 解释内容
     * 
     * @param content     要解释的内容
     * @param type        类型 (git-command/code)
     * @param language    输出语言
     * @param detailLevel 详细程度
     * @param model       AI 模型
     * @return 解释结果
     */
    public String explain(String content, String type, String language, String detailLevel, String model) {
        String systemPrompt = buildSystemPrompt(type, language, detailLevel);
        String userMessage = buildUserMessage(content, type);
        
        return aiService.chat(model, systemPrompt, userMessage);
    }

    private String buildSystemPrompt(String type, String language, String detailLevel) {
        String langInstruction = language.equals("zh") 
            ? "请用中文回答。" 
            : "Please answer in English.";
        
        String detailInstruction = detailLevel.equals("detailed")
            ? "提供详细的解释，包括：原理、使用场景、注意事项、示例。"
            : "提供简洁的解释，一两句话说明核心功能。";

        String typeContext = switch (type) {
            case "git-command" -> """
                你是一个 Git 专家，擅长解释各种 Git 命令和工作流。
                解释时要说明：
                1. 命令的作用
                2. 各个参数的含义
                3. 执行后的效果
                4. 可能的风险（如果有）
                5. 相关的替代命令
                """;
            case "code" -> """
                你是一个资深软件工程师，擅长解释各种编程语言的代码。
                解释时要说明：
                1. 代码的功能
                2. 关键逻辑
                3. 设计模式（如果使用了）
                4. 潜在问题（如果有）
                """;
            default -> "你是一个技术专家，擅长解释技术概念。";
        };

        return typeContext + "\n\n" + langInstruction + "\n" + detailInstruction;
    }

    private String buildUserMessage(String content, String type) {
        return switch (type) {
            case "git-command" -> "请解释这个 Git 命令：\n\n" + content;
            case "code" -> "请解释这段代码：\n\n```\n" + content + "\n```";
            default -> "请解释：\n\n" + content;
        };
    }
}
