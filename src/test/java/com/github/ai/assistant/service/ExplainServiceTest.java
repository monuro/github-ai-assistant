package com.github.ai.assistant.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * ExplainService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExplainService 测试")
class ExplainServiceTest {

    @Mock
    private AIService aiService;

    private ExplainService explainService;

    @BeforeEach
    void setUp() {
        explainService = new ExplainService(aiService);
    }

    @Nested
    @DisplayName("explain 方法测试")
    class ExplainTests {

        @Test
        @DisplayName("解释 Git 命令时应使用正确的类型")
        void shouldExplainGitCommand() {
            // Given
            String command = "git rebase -i HEAD~3";
            String expectedExplanation = "这是一个交互式 rebase 命令...";
            when(aiService.chat(anyString(), anyString(), anyString()))
                .thenReturn(expectedExplanation);

            // When
            String result = explainService.explain(command, "git-command", "zh", "detailed", "openai");

            // Then
            assertEquals(expectedExplanation, result);
            
            // 验证调用参数
            ArgumentCaptor<String> systemPromptCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> userMessageCaptor = ArgumentCaptor.forClass(String.class);
            verify(aiService).chat(eq("openai"), systemPromptCaptor.capture(), userMessageCaptor.capture());
            
            String systemPrompt = systemPromptCaptor.getValue();
            String userMessage = userMessageCaptor.getValue();
            
            assertTrue(systemPrompt.contains("Git 专家"));
            assertTrue(userMessage.contains(command));
        }

        @Test
        @DisplayName("解释代码时应使用正确的类型")
        void shouldExplainCode() {
            // Given
            String code = "public class Hello { }";
            String expectedExplanation = "这是一个 Java 类定义...";
            when(aiService.chat(anyString(), anyString(), anyString()))
                .thenReturn(expectedExplanation);

            // When
            String result = explainService.explain(code, "code", "zh", "detailed", "openai");

            // Then
            assertEquals(expectedExplanation, result);
            
            ArgumentCaptor<String> systemPromptCaptor = ArgumentCaptor.forClass(String.class);
            verify(aiService).chat(anyString(), systemPromptCaptor.capture(), anyString());
            
            assertTrue(systemPromptCaptor.getValue().contains("软件工程师"));
        }

        @ParameterizedTest
        @DisplayName("应支持不同的语言设置")
        @CsvSource({
            "zh, 中文",
            "en, English"
        })
        void shouldSupportDifferentLanguages(String language, String expectedKeyword) {
            // Given
            when(aiService.chat(anyString(), anyString(), anyString()))
                .thenReturn("explanation");

            // When
            explainService.explain("test", "git-command", language, "simple", "openai");

            // Then
            ArgumentCaptor<String> systemPromptCaptor = ArgumentCaptor.forClass(String.class);
            verify(aiService).chat(anyString(), systemPromptCaptor.capture(), anyString());
            
            String systemPrompt = systemPromptCaptor.getValue();
            assertTrue(
                systemPrompt.contains(expectedKeyword) || 
                (language.equals("zh") && systemPrompt.contains("中文")) ||
                (language.equals("en") && systemPrompt.contains("English")),
                "System prompt should contain language instruction"
            );
        }

        @ParameterizedTest
        @DisplayName("应支持不同的详细程度")
        @CsvSource({
            "simple, 简洁",
            "detailed, 详细"
        })
        void shouldSupportDifferentDetailLevels(String detailLevel, String expectedKeyword) {
            // Given
            when(aiService.chat(anyString(), anyString(), anyString()))
                .thenReturn("explanation");

            // When
            explainService.explain("test", "git-command", "zh", detailLevel, "openai");

            // Then
            ArgumentCaptor<String> systemPromptCaptor = ArgumentCaptor.forClass(String.class);
            verify(aiService).chat(anyString(), systemPromptCaptor.capture(), anyString());
            
            assertTrue(systemPromptCaptor.getValue().contains(expectedKeyword));
        }

        @Test
        @DisplayName("使用 Ollama 模型时应正确传递")
        void shouldUseOllamaModel() {
            // Given
            when(aiService.chat(anyString(), anyString(), anyString()))
                .thenReturn("explanation");

            // When
            explainService.explain("test", "code", "zh", "simple", "ollama");

            // Then
            verify(aiService).chat(eq("ollama"), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("边界情况测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("未知类型应使用默认处理")
        void shouldHandleUnknownType() {
            // Given
            when(aiService.chat(anyString(), anyString(), anyString()))
                .thenReturn("explanation");

            // When
            String result = explainService.explain("something", "unknown-type", "zh", "simple", "openai");

            // Then
            assertNotNull(result);
            verify(aiService).chat(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("空内容应正常处理")
        void shouldHandleEmptyContent() {
            // Given
            when(aiService.chat(anyString(), anyString(), anyString()))
                .thenReturn("无法解释空内容");

            // When
            String result = explainService.explain("", "code", "zh", "simple", "openai");

            // Then
            assertNotNull(result);
        }
    }
}
