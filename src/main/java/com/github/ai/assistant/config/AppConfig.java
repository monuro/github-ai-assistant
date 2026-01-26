package com.github.ai.assistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 应用配置类
 * 
 * 绑定 application.yml 中的配置
 */
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {

    private GitHub github = new GitHub();
    private AI ai = new AI();

    public GitHub getGithub() {
        return github;
    }

    public void setGithub(GitHub github) {
        this.github = github;
    }

    public AI getAi() {
        return ai;
    }

    public void setAi(AI ai) {
        this.ai = ai;
    }

    /**
     * GitHub 相关配置
     */
    public static class GitHub {
        private String token;
        private String apiUrl = "https://api.github.com";
        private String defaultRepository;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getApiUrl() {
            return apiUrl;
        }

        public void setApiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
        }

        public String getDefaultRepository() {
            return defaultRepository;
        }

        public void setDefaultRepository(String defaultRepository) {
            this.defaultRepository = defaultRepository;
        }
    }

    /**
     * AI 相关配置
     */
    public static class AI {
        private String defaultModel = "openai";
        private String defaultLanguage = "zh";

        public String getDefaultModel() {
            return defaultModel;
        }

        public void setDefaultModel(String defaultModel) {
            this.defaultModel = defaultModel;
        }

        public String getDefaultLanguage() {
            return defaultLanguage;
        }

        public void setDefaultLanguage(String defaultLanguage) {
            this.defaultLanguage = defaultLanguage;
        }
    }
}
