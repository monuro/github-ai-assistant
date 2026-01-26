package com.github.ai.assistant.service;

import com.github.ai.assistant.client.GitHubClientService;
import com.github.ai.assistant.model.IssueClassification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kohsuke.github.GHIssue;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * IssueService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IssueService 测试")
class IssueServiceTest {

    @Mock
    private AIService aiService;

    @Mock
    private GitHubClientService githubClient;

    @Mock
    private GHIssue mockIssue;

    private IssueService issueService;

    @BeforeEach
    void setUp() {
        issueService = new IssueService(aiService, githubClient);
    }

    @Nested
    @DisplayName("classifyIssue 测试")
    class ClassifyIssueTests {

        @Test
        @DisplayName("应成功分类 Issue")
        void shouldClassifyIssueSuccessfully() throws IOException {
            // Given
            when(githubClient.getIssue("owner/repo", 1)).thenReturn(mockIssue);
            when(mockIssue.getTitle()).thenReturn("App crashes on startup");
            when(mockIssue.getBody()).thenReturn("The app crashes when I open it...");
            
            String aiResponse = """
                类型: bug
                优先级: high
                标签: bug, crash, needs-investigation
                理由: 应用启动时崩溃是严重的 bug
                """;
            when(aiService.chat(anyString(), anyString(), anyString())).thenReturn(aiResponse);

            // When
            IssueClassification result = issueService.classifyIssue("owner/repo", 1, "openai");

            // Then
            assertEquals("bug", result.type());
            assertEquals("high", result.priority());
            assertTrue(result.suggestedLabels().contains("bug"));
            assertTrue(result.suggestedLabels().contains("crash"));
            assertFalse(result.reasoning().isBlank());
        }

        @Test
        @DisplayName("应正确传递 Issue 信息到 AI")
        void shouldPassIssueInfoToAI() throws IOException {
            // Given
            when(githubClient.getIssue(anyString(), anyInt())).thenReturn(mockIssue);
            when(mockIssue.getTitle()).thenReturn("Feature Request: Dark Mode");
            when(mockIssue.getBody()).thenReturn("Please add dark mode support");
            when(aiService.chat(anyString(), anyString(), anyString()))
                .thenReturn("类型: feature\n优先级: low\n标签: enhancement\n理由: 功能请求");

            // When
            issueService.classifyIssue("owner/repo", 123, "openai");

            // Then
            ArgumentCaptor<String> userMessageCaptor = ArgumentCaptor.forClass(String.class);
            verify(aiService).chat(anyString(), anyString(), userMessageCaptor.capture());
            
            String userMessage = userMessageCaptor.getValue();
            assertTrue(userMessage.contains("Feature Request: Dark Mode"));
            assertTrue(userMessage.contains("dark mode"));
        }

        @Test
        @DisplayName("当 GitHub API 失败时应抛出 RuntimeException")
        void shouldThrowRuntimeExceptionWhenGitHubApiFails() throws IOException {
            // Given
            when(githubClient.getIssue(anyString(), anyInt()))
                .thenThrow(new IOException("API Error"));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> issueService.classifyIssue("owner/repo", 1, "openai"));
            
            assertTrue(exception.getMessage().contains("获取 Issue 失败"));
        }
    }

    @Nested
    @DisplayName("suggestReply 测试")
    class SuggestReplyTests {

        @Test
        @DisplayName("应生成友善的回复建议")
        void shouldGenerateFriendlyReplySuggestion() throws IOException {
            // Given
            when(githubClient.getIssue("owner/repo", 1)).thenReturn(mockIssue);
            when(mockIssue.getTitle()).thenReturn("Bug: Button not working");
            when(mockIssue.getBody()).thenReturn("The submit button does nothing");
            
            String expectedReply = "感谢您的反馈！我们会尽快调查这个问题。";
            when(aiService.chat(anyString(), anyString(), anyString())).thenReturn(expectedReply);

            // When
            String result = issueService.suggestReply("owner/repo", 1, "openai");

            // Then
            assertEquals(expectedReply, result);
        }

        @Test
        @DisplayName("系统提示词应包含友善要求")
        void shouldIncludeFriendlyRequirementInPrompt() throws IOException {
            // Given
            when(githubClient.getIssue(anyString(), anyInt())).thenReturn(mockIssue);
            when(mockIssue.getTitle()).thenReturn("Question");
            when(mockIssue.getBody()).thenReturn("How to use?");
            when(aiService.chat(anyString(), anyString(), anyString())).thenReturn("reply");

            // When
            issueService.suggestReply("owner/repo", 1, "openai");

            // Then
            ArgumentCaptor<String> systemPromptCaptor = ArgumentCaptor.forClass(String.class);
            verify(aiService).chat(anyString(), systemPromptCaptor.capture(), anyString());
            
            String systemPrompt = systemPromptCaptor.getValue();
            assertTrue(systemPrompt.contains("友善"));
            assertTrue(systemPrompt.contains("专业"));
        }
    }

    @Nested
    @DisplayName("summarizeOpenIssues 测试")
    class SummarizeOpenIssuesTests {

        @Test
        @DisplayName("当没有 Open Issue 时应返回提示信息")
        void shouldReturnMessageWhenNoOpenIssues() throws IOException {
            // Given
            when(githubClient.getOpenIssues("owner/repo")).thenReturn(List.of());

            // When
            String result = issueService.summarizeOpenIssues("owner/repo", "openai");

            // Then
            assertTrue(result.contains("没有"));
            verify(aiService, never()).chat(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("应汇总多个 Open Issues")
        void shouldSummarizeMultipleIssues() throws IOException {
            // Given
            GHIssue issue1 = mock(GHIssue.class);
            GHIssue issue2 = mock(GHIssue.class);
            
            when(issue1.getNumber()).thenReturn(1);
            when(issue1.getTitle()).thenReturn("Bug 1");
            when(issue2.getNumber()).thenReturn(2);
            when(issue2.getTitle()).thenReturn("Feature 2");
            
            when(githubClient.getOpenIssues("owner/repo")).thenReturn(List.of(issue1, issue2));
            
            String expectedSummary = "共有 2 个 Open Issues...";
            when(aiService.chat(anyString(), anyString(), anyString())).thenReturn(expectedSummary);

            // When
            String result = issueService.summarizeOpenIssues("owner/repo", "openai");

            // Then
            assertEquals(expectedSummary, result);
            
            ArgumentCaptor<String> userMessageCaptor = ArgumentCaptor.forClass(String.class);
            verify(aiService).chat(anyString(), anyString(), userMessageCaptor.capture());
            
            String userMessage = userMessageCaptor.getValue();
            assertTrue(userMessage.contains("#1"));
            assertTrue(userMessage.contains("#2"));
        }

        @Test
        @DisplayName("应限制汇总的 Issue 数量")
        void shouldLimitNumberOfIssuesToSummarize() throws IOException {
            // Given: 创建 25 个 mock issues，使用 lenient 避免 UnnecessaryStubbingException
            List<GHIssue> manyIssues = new java.util.ArrayList<>();
            for (int i = 0; i < 25; i++) {
                GHIssue issue = mock(GHIssue.class);
                // 使用 lenient() 因为代码只会使用前 20 个
                lenient().when(issue.getNumber()).thenReturn(i);
                lenient().when(issue.getTitle()).thenReturn("Issue " + i);
                manyIssues.add(issue);
            }
            
            when(githubClient.getOpenIssues("owner/repo")).thenReturn(manyIssues);
            when(aiService.chat(anyString(), anyString(), anyString())).thenReturn("summary");

            // When
            issueService.summarizeOpenIssues("owner/repo", "openai");

            // Then
            ArgumentCaptor<String> userMessageCaptor = ArgumentCaptor.forClass(String.class);
            verify(aiService).chat(anyString(), anyString(), userMessageCaptor.capture());
            
            // 应该只包含前 20 个 issues
            String userMessage = userMessageCaptor.getValue();
            assertTrue(userMessage.contains("#19")); // 第 20 个
            assertFalse(userMessage.contains("#20")); // 第 21 个不应该出现
        }
    }

    @Nested
    @DisplayName("IssueClassification Record 测试")
    class IssueClassificationTests {

        @Test
        @DisplayName("应正确创建 IssueClassification")
        void shouldCreateIssueClassification() {
            // When
            IssueClassification classification = new IssueClassification(
                "bug",
                "high",
                List.of("bug", "critical"),
                "这是一个严重的 bug"
            );

            // Then
            assertEquals("bug", classification.type());
            assertEquals("high", classification.priority());
            assertEquals(2, classification.suggestedLabels().size());
            assertEquals("这是一个严重的 bug", classification.reasoning());
        }

        @Test
        @DisplayName("IssueType 枚举应包含所有类型")
        void shouldContainAllIssueTypes() {
            // Then
            assertEquals("bug", IssueClassification.IssueType.BUG.getValue());
            assertEquals("feature", IssueClassification.IssueType.FEATURE.getValue());
            assertEquals("enhancement", IssueClassification.IssueType.ENHANCEMENT.getValue());
            assertEquals("question", IssueClassification.IssueType.QUESTION.getValue());
            assertEquals("documentation", IssueClassification.IssueType.DOCUMENTATION.getValue());
            assertEquals("other", IssueClassification.IssueType.OTHER.getValue());
        }

        @Test
        @DisplayName("Priority 枚举应包含所有优先级")
        void shouldContainAllPriorityLevels() {
            // Then
            assertEquals("low", IssueClassification.Priority.LOW.getValue());
            assertEquals("medium", IssueClassification.Priority.MEDIUM.getValue());
            assertEquals("high", IssueClassification.Priority.HIGH.getValue());
            assertEquals("critical", IssueClassification.Priority.CRITICAL.getValue());
        }
    }
}
