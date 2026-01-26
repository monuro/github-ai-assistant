package com.github.ai.assistant.service;

import com.github.ai.assistant.service.CommitService.DiffStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * CommitService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CommitService 测试")
class CommitServiceTest {

    @Mock
    private AIService aiService;

    private CommitService commitService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        commitService = new CommitService(aiService);
    }

    @Nested
    @DisplayName("generateCommitMessage 测试")
    class GenerateCommitMessageTests {

        @Test
        @DisplayName("当有 staged 变更时，应成功生成 commit message")
        void shouldGenerateCommitMessageWhenStagedChangesExist() throws Exception {
            // Given: 初始化一个 Git 仓库并添加文件
            initGitRepo();
            createAndStageFile("test.txt", "Hello World");
            
            String expectedMessage = "feat: add test file";
            when(aiService.chat(anyString(), anyString(), anyString()))
                .thenReturn(expectedMessage);

            // When
            String result = commitService.generateCommitMessage(
                tempDir, "zh", "conventional", "openai"
            );

            // Then
            assertEquals(expectedMessage, result);
            verify(aiService).chat(eq("openai"), anyString(), anyString());
        }

        @Test
        @DisplayName("当没有 staged 变更时，应抛出异常")
        void shouldThrowExceptionWhenNoStagedChanges() throws Exception {
            // Given: 初始化一个空的 Git 仓库
            initGitRepo();

            // When & Then
            IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> commitService.generateCommitMessage(tempDir, "zh", "conventional", "openai")
            );
            
            assertTrue(exception.getMessage().contains("没有 staged"));
        }
    }

    @Nested
    @DisplayName("executeCommit 测试")
    class ExecuteCommitTests {

        @Test
        @DisplayName("当 commit 成功时，应返回成功结果")
        void shouldReturnSuccessWhenCommitSucceeds() throws Exception {
            // Given
            initGitRepo();
            createAndStageFile("test.txt", "Hello World");

            // When
            var result = commitService.executeCommit(tempDir, "test: initial commit");

            // Then
            assertTrue(result.success());
            assertNotNull(result.commitHash());
            assertFalse(result.commitHash().isBlank());
        }

        @Test
        @DisplayName("当没有 staged 文件时，commit 应失败")
        void shouldFailWhenNoStagedFiles() throws Exception {
            // Given
            initGitRepo();

            // When
            var result = commitService.executeCommit(tempDir, "test: empty commit");

            // Then
            assertFalse(result.success());
        }
    }

    @Nested
    @DisplayName("getStagedFiles 测试")
    class GetStagedFilesTests {

        @Test
        @DisplayName("应返回所有 staged 文件列表")
        void shouldReturnStagedFilesList() throws Exception {
            // Given
            initGitRepo();
            createAndStageFile("file1.txt", "content1");
            createAndStageFile("file2.txt", "content2");

            // When
            var files = commitService.getStagedFiles(tempDir);

            // Then
            assertEquals(2, files.size());
            assertTrue(files.contains("file1.txt"));
            assertTrue(files.contains("file2.txt"));
        }

        @Test
        @DisplayName("当没有 staged 文件时，应返回空列表")
        void shouldReturnEmptyListWhenNoStagedFiles() throws Exception {
            // Given
            initGitRepo();

            // When
            var files = commitService.getStagedFiles(tempDir);

            // Then
            assertTrue(files.isEmpty());
        }
    }

    @Nested
    @DisplayName("getStagedStats 测试")
    class GetStagedStatsTests {

        @Test
        @DisplayName("应正确统计 staged 变更")
        void shouldReturnCorrectStats() throws Exception {
            // Given
            initGitRepo();
            createAndStageFile("test.txt", "line1\nline2\nline3");

            // When
            DiffStats stats = commitService.getStagedStats(tempDir);

            // Then
            assertEquals(1, stats.filesChanged());
            assertEquals(3, stats.insertions());
            assertEquals(0, stats.deletions());
        }
    }

    @Nested
    @DisplayName("DiffStats Record 测试")
    class DiffStatsTests {

        @Test
        @DisplayName("totalChanges 应返回插入和删除的总和")
        void shouldCalculateTotalChanges() {
            // Given
            DiffStats stats = new DiffStats(5, 100, 50);

            // When
            int total = stats.totalChanges();

            // Then
            assertEquals(150, total);
        }
    }

    // ============ Helper Methods ============

    /**
     * 初始化 Git 仓库
     */
    private void initGitRepo() throws IOException, InterruptedException {
        runGitCommand("init");
        runGitCommand("config", "user.email", "test@example.com");
        runGitCommand("config", "user.name", "Test User");
    }

    /**
     * 创建文件并 stage
     */
    private void createAndStageFile(String filename, String content) throws IOException, InterruptedException {
        Path filePath = tempDir.resolve(filename);
        Files.writeString(filePath, content);
        runGitCommand("add", filename);
    }

    /**
     * 运行 Git 命令
     */
    private void runGitCommand(String... args) throws IOException, InterruptedException {
        String[] command = new String[args.length + 1];
        command[0] = "git";
        System.arraycopy(args, 0, command, 1, args.length);
        
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(tempDir.toFile());
        pb.redirectErrorStream(true);
        
        Process process = pb.start();
        process.waitFor();
    }
}
