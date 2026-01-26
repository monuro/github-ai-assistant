package com.github.ai.assistant.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * AI 模型配置
 * 
 * 配置 OpenAI 和 Ollama 两种 AI 提供者
 */
@Configuration
public class AIConfig {

    @Value("${spring.ai.openai.api-key:}")
    private String openaiApiKey;

    @Value("${spring.ai.openai.base-url:https://api.openai.com}")
    private String openaiBaseUrl;

    @Value("${spring.ai.openai.chat.options.model:gpt-4o-mini}")
    private String openaiModel;

    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${spring.ai.ollama.chat.options.model:llama3}")
    private String ollamaModel;

    /**
     * OpenAI ChatModel
     */
    @Bean(name = "openAiChatModel")
    @Primary
    public ChatModel openAiChatModel() {
        if (openaiApiKey == null || openaiApiKey.isBlank()) {
            // 返回一个占位符，实际使用时会报错提示配置 API Key
            return new PlaceholderChatModel("OpenAI API Key 未配置，请设置环境变量 OPENAI_API_KEY 或在 application.yml 中配置");
        }
        
        OpenAiApi openAiApi = new OpenAiApi(openaiBaseUrl, openaiApiKey);
        
        OpenAiChatOptions options = OpenAiChatOptions.builder()
            .withModel(openaiModel)
            .withTemperature(0.7)
            .build();
        
        return new OpenAiChatModel(openAiApi, options);
    }

    /**
     * Ollama ChatModel (本地模型)
     */
    @Bean(name = "ollamaChatModel")
    public ChatModel ollamaChatModel() {
        OllamaApi ollamaApi = new OllamaApi(ollamaBaseUrl);
        
        OllamaOptions options = OllamaOptions.builder()
            .withModel(ollamaModel)
            .withTemperature(0.7)
            .build();
        
        return OllamaChatModel.builder()
            .withOllamaApi(ollamaApi)
            .withDefaultOptions(options)
            .build();
    }

    /**
     * 占位符 ChatModel，用于未配置 API Key 时给出友好提示
     */
    private static class PlaceholderChatModel implements ChatModel {
        private final String errorMessage;

        public PlaceholderChatModel(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            throw new IllegalStateException(errorMessage);
        }

        @Override
        public ChatOptions getDefaultOptions() {
            return null;
        }
    }
}
