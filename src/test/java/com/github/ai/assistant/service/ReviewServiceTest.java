package com.github.ai.assistant.service;

import com.github.ai.assistant.client.GitHubClientService;
import com.github.ai.assistant.model.PullRequestInfo;
import com.github.ai.assistant.model.PullRequestInfo.FileChange;
import com.github.ai.assistant.model.ReviewResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * ReviewService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewService 测试")
class ReviewServiceTest {

    @Mock
    private AIService aiService;

    @Mock
    private GitHubClientService githubClient;

    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(aiService, githubClient);
    }

    @Nested
    @DisplayName("reviewPullRequest 测试")
    class ReviewPullRequestTests {

        @Test
        @DisplayName("应成功审查 PR 并返回结果")
        void shouldReviewPullRequestSuccessfully() throws IOException {
            // Given
            PullRequestInfo prInfo = createMockPRInfo();
            when(githubClient.getPullRequest("owner/repo", 123)).thenReturn(prInfo);
            
            String aiResponse = """
                ## 总结
                这是一个添加新功能的 PR
                
                ## 评分
                85
                
                ## 问题
                - 缺少单元测试
                - 变量命名不够清晰
                
                ## 建议
                - 添加测试覆盖
                - 使用更有意义的变量名
                """;
            when(aiService.chat(anyString(), anyString(), anyString())).thenReturn(aiResponse);

            // When
            ReviewResult result = reviewService.reviewPullRequest("owner/repo", 123, "all", "openai");

            // Then
            assertNotNull(result);
            assertEquals(85, result.score());
            assertFalse(result.issues().isEmpty());
            assertFalse(result.suggestions().isEmpty());
            
            verify(githubClient).getPullRequest("owner/repo", 123);
            verify(aiService).chat(eq("openai"), anyString(), anyString());
        }

        @ParameterizedTest
        @DisplayName("应支持不同的审查重点")
        @ValueSource(strings = {"security", "performance", "style", "all"})
        void shouldSupportDifferentFocusTypes(String focus) throws IOException {
            // Given
            when(githubClient.getPullRequest(anyString(), anyInt())).thenReturn(createMockPRInfo());
            when(aiService.chat(anyString(), anyString(), anyString())).thenReturn(createMockAIResponse(80));

            // When
            reviewService.reviewPullRequest("owner/repo", 1, focus, "openai");

            // Then
            ArgumentCaptor<String> systemPromptCaptor = ArgumentCaptor.forClass(String.class);
            verify(aiService).chat(anyString(), systemPromptCaptor.capture(), anyString());
            
            String systemPrompt = systemPromptCaptor.getValue();
            switch (focus) {
                case "security" -> assertTrue(systemPrompt.contains("安全"));
                case "performance" -> assertTrue(systemPrompt.contains("性能"));
                case "style" -> assertTrue(systemPrompt.contains("风格"));
                case "all" -> assertTrue(systemPrompt.contains("全面"));
            }
        }

        @Test
        @DisplayName("当 GitHub API 失败时应抛出异常")
        void shouldThrowExceptionWhenGitHubApiFails() throws IOException {
            // Given
            when(githubClient.getPullRequest(anyString(), anyInt()))
                .thenThrow(new IOException("API Error"));

            // When & Then
            assertThrows(IOException.class, 
                () -> reviewService.reviewPullRequest("owner/repo", 123, "all", "openai"));
        }
    }

    @Nested
    @DisplayName("结果解析测试")
    class ResultParsingTests {

        @Test
        @DisplayName("应正确解析评分")
        void shouldParseScoreCorrectly() throws IOException {
            // Given
            when(githubClient.getPullRequest(anyString(), anyInt())).thenReturn(createMockPRInfo());
            when(aiService.chat(anyString(), anyString(), anyString()))
                .thenReturn(createMockAIResponse(92));

            // When
            ReviewResult result = reviewService.reviewPullRequest("owner/repo", 1, "all", "openai");

            // Then
            assertEquals(92, result.score());
        }

        @Test
        @DisplayName("当评分解析失败时应使用默认值")
        void shouldUseDefaultScoreWhenParsingFails() throws IOException {
            // Given
            when(githubClient.getPullRequest(anyString(), anyInt())).thenReturn(createMockPRInfo());
            
            String responseWithoutScore = """
                ## 总结
                测试 PR
                
                ## 评分
                无法评分
                
                ## 问题
                
                ## 建议
                """;
            when(aiService.chat(anyString(), anyString(), anyString())).thenReturn(responseWithoutScore);

            // When
            ReviewResult result = reviewService.reviewPullRequest("owner/repo", 1, "all", "openai");

            // Then
            assertEquals(70, result.score()); // 默认分数
        }

        @Test
        @DisplayName("应正确解析问题列表")
        void shouldParseIssuesCorrectly() throws IOException {
            // Given
            when(githubClient.getPullRequest(anyString(), anyInt())).thenReturn(createMockPRInfo());
            
            String response = """
                ## 总结
                测试
                
                ## 评分
                75
                
                ## 问题
                - 问题一
                - 问题二
                - 问题三
                
                ## 建议
                - 建议一
                """;
            when(aiService.chat(anyString(), anyString(), anyString())).thenReturn(response);

            // When
            ReviewResult result = reviewService.reviewPullRequest("owner/repo", 1, "all", "openai");

            // Then
            assertEquals(3, result.issues().size());
            assertTrue(result.issues().contains("问题一"));
            assertTrue(result.issues().contains("问题二"));
            assertTrue(result.issues().contains("问题三"));
        }
    }

    @Nested
    @DisplayName("ReviewResult Record 测试")
    class ReviewResultTests {

        @Test
        @DisplayName("empty() 应返回空结果")
        void shouldCreateEmptyResult() {
            // When
            ReviewResult empty = ReviewResult.empty();

            // Then
            assertEquals("", empty.summary());
            assertEquals(0, empty.score());
            assertTrue(empty.issues().isEmpty());
            assertTrue(empty.suggestions().isEmpty());
        }
    }

    // ============ Helper Methods ============

    private PullRequestInfo createMockPRInfo() {
        return new PullRequestInfo(
            123,
            "Test PR",
            "This is a test PR",
            "testuser",
            "OPEN",
            "main",
            "feature/test",
            Instant.now(),
            List.of(new FileChange("test.java", "modified", 10, 5, "diff content")),
            "--- test.java\n+++ test.java\n@@ -1,5 +1,10 @@"
        );
    }

    private String createMockAIResponse(int score) {
        return """
            ## 总结
            这是一个测试 PR
            
            ## 评分
            %d
            
            ## 问题
            - 测试问题
            
            ## 建议
            - 测试建议
            """.formatted(score);
    }
}
