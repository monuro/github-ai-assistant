package com.github.ai.assistant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 应用主测试类
 * 
 * 验证 Spring Boot 应用上下文是否能正常加载
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("GithubAiAssistantApplication 测试")
class GithubAiAssistantApplicationTests {

    @Test
    @DisplayName("应用上下文应能正常加载")
    void contextLoads() {
        // 如果上下文加载成功，测试就通过
    }
}
