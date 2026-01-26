package com.github.ai.assistant.service;

import com.github.ai.assistant.config.AppConfig;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * AI 服务基类
 * 
 * 提供统一的 AI 调用接口，支持多种模型：
 * - OpenAI (GPT-4, GPT-3.5)
 * - Ollama (本地模型)
 */
@Service
public class AIService {

    private final ChatClient openAiClient;
    private final ChatClient ollamaClient;
    private final AppConfig appConfig;

    public AIService(
            @Qualifier("openAiChatModel") ChatModel openAiChatModel,
            @Qualifier("ollamaChatModel") ChatModel ollamaChatModel,
            AppConfig appConfig) {
        this.openAiClient = ChatClient.create(openAiChatModel);
        this.ollamaClient = ChatClient.create(ollamaChatModel);
        this.appConfig = appConfig;
    }

    /**
     * 根据模型名称获取对应的 ChatClient
     */
    public ChatClient getClient(String model) {
        return switch (model.toLowerCase()) {
            case "ollama", "local" -> ollamaClient;
            case "openai", "gpt" -> openAiClient;
            default -> openAiClient;  // 默认使用 OpenAI
        };
    }

    /**
     * 执行 AI 对话
     */
    public String chat(String model, String systemPrompt, String userMessage) {
        return getClient(model)
            .prompt()
            .system(systemPrompt)
            .user(userMessage)
            .call()
            .content();
    }

    /**
     * 执行 AI 对话并返回结构化结果
     */
    public <T> T chat(String model, String systemPrompt, String userMessage, Class<T> responseType) {
        return getClient(model)
            .prompt()
            .system(systemPrompt)
            .user(userMessage)
            .call()
            .entity(responseType);
    }
}
