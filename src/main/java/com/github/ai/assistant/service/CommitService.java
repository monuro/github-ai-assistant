package com.github.ai.assistant.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Commit Message 生成服务
 * 
 * 根据 Git diff 生成规范的 commit message
 */
@Service
public class CommitService {

    private final AIService aiService;

    public CommitService(AIService aiService) {
        this.aiService = aiService;
    }

    /**
     * 生成 Commit Message
     * 
     * @param repoPath Git 仓库路径
     * @param language 语言 (zh/en)
     * @param type     类型 (conventional/simple)
     * @param model    AI 模型
     * @return 生成的 commit message
     */
    public String generateCommitMessage(Path repoPath, String language, String type, String model) 
            throws IOException, InterruptedException {
        
        // 获取 staged 的 diff
        String diff = getStagedDiff(repoPath);
        
        if (diff.isBlank()) {
            throw new IllegalStateException("没有 staged 的变更。请先使用 git add 添加文件。");
        }
        
        String systemPrompt = buildSystemPrompt(language, type);
        String userMessage = buildUserMessage(diff);
        
        return aiService.chat(model, systemPrompt, userMessage);
    }

    /**
     * 执行 Git Commit
     * 
     * @param repoPath 仓库路径
     * @param message  commit message
     * @return commit 结果信息
     */
    public CommitResult executeCommit(Path repoPath, String message) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("git", "commit", "-m", message);
        pb.directory(repoPath.toFile());
        pb.redirectErrorStream(true);
        
        Process process = pb.start();
        
        String output;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            output = reader.lines().collect(Collectors.joining("\n"));
        }
        
        int exitCode = process.waitFor();
        
        if (exitCode == 0) {
            // 获取 commit hash
            String hash = getLastCommitHash(repoPath);
            return new CommitResult(true, hash, output);
        } else {
            return new CommitResult(false, null, output);
        }
    }

    /**
     * 获取最新的 commit hash
     */
    private String getLastCommitHash(Path repoPath) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("git", "rev-parse", "--short", "HEAD");
        pb.directory(repoPath.toFile());
        
        Process process = pb.start();
        
        String output;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            output = reader.lines().collect(Collectors.joining()).trim();
        }
        
        process.waitFor();
        return output;
    }

    /**
     * 获取 staged 文件列表
     */
    public List<String> getStagedFiles(Path repoPath) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("git", "diff", "--cached", "--name-only");
        pb.directory(repoPath.toFile());
        
        Process process = pb.start();
        
        List<String> files;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            files = reader.lines()
                .filter(line -> !line.isBlank())
                .collect(Collectors.toList());
        }
        
        process.waitFor();
        return files;
    }

    /**
     * 获取 staged 文件的 diff
     */
    public String getStagedDiff(Path repoPath) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("git", "diff", "--cached");
        pb.directory(repoPath.toFile());
        pb.redirectErrorStream(true);
        
        Process process = pb.start();
        
        String output;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            output = reader.lines().collect(Collectors.joining("\n"));
        }
        
        process.waitFor();
        return output;
    }

    /**
     * 获取 staged 文件的统计信息
     */
    public DiffStats getStagedStats(Path repoPath) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("git", "diff", "--cached", "--stat");
        pb.directory(repoPath.toFile());
        
        Process process = pb.start();
        
        String output;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            output = reader.lines().collect(Collectors.joining("\n"));
        }
        
        process.waitFor();
        
        // 解析统计信息
        return parseDiffStats(output);
    }

    /**
     * 解析 diff 统计信息
     */
    private DiffStats parseDiffStats(String statOutput) {
        int filesChanged = 0;
        int insertions = 0;
        int deletions = 0;
        
        // 最后一行格式: "X files changed, Y insertions(+), Z deletions(-)"
        String[] lines = statOutput.split("\n");
        if (lines.length > 0) {
            String lastLine = lines[lines.length - 1].trim();
            
            // 解析文件数
            if (lastLine.contains("file")) {
                try {
                    String[] parts = lastLine.split(",");
                    for (String part : parts) {
                        part = part.trim();
                        if (part.contains("file")) {
                            filesChanged = Integer.parseInt(part.split(" ")[0]);
                        } else if (part.contains("insertion")) {
                            insertions = Integer.parseInt(part.split(" ")[0]);
                        } else if (part.contains("deletion")) {
                            deletions = Integer.parseInt(part.split(" ")[0]);
                        }
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        
        return new DiffStats(filesChanged, insertions, deletions);
    }

    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt(String language, String type) {
        boolean isChinese = language.equals("zh");
        
        String typeInstruction = type.equals("conventional") 
            ? """
              使用 Conventional Commits 格式：
              <type>(<scope>): <subject>
              
              <body>
              
              类型包括：feat, fix, docs, style, refactor, test, chore
              """
            : (isChinese ? "生成简洁的一行 commit message。" : "Generate a concise one-line commit message.");
        
        if (isChinese) {
            return """
                你是一个专业的软件工程师，擅长写清晰、规范的 Git commit message。
                
                **重要：必须使用中文生成 commit message 的 subject 和 body 部分。**
                
                %s
                
                规则：
                1. type 和 scope 保持英文（如 feat, fix, chore）
                2. subject 和 body 必须用中文
                3. subject 不超过 50 个字符
                4. body 解释"为什么"而不是"做了什么"
                5. 不要在末尾加句号
                
                示例：
                chore(build): 移除 Docker 相关配置
                
                项目不再需要 Docker 部署方式，简化构建流程。
                """.formatted(typeInstruction);
        } else {
            return """
                You are a professional software engineer skilled at writing clear, standard Git commit messages.
                
                %s
                
                Rules:
                1. Subject should not exceed 50 characters
                2. Body explains "why" not "what"
                3. Use imperative mood (e.g., "Add feature" not "Added feature")
                4. Do not end with a period
                """.formatted(typeInstruction);
        }
    }

    /**
     * 构建用户消息
     */
    private String buildUserMessage(String diff) {
        return """
            请根据以下 Git diff 生成 commit message：
            
            ```diff
            %s
            ```
            
            只输出 commit message，不要其他解释。
            """.formatted(truncateDiff(diff, 4000));
    }

    /**
     * 截断过长的 diff
     */
    private String truncateDiff(String diff, int maxLength) {
        if (diff.length() <= maxLength) {
            return diff;
        }
        return diff.substring(0, maxLength) + "\n... (diff truncated)";
    }

    /**
     * Commit 执行结果
     */
    public record CommitResult(
        boolean success,
        String commitHash,
        String message
    ) {}

    /**
     * Diff 统计信息
     */
    public record DiffStats(
        int filesChanged,
        int insertions,
        int deletions
    ) {
        public int totalChanges() {
            return insertions + deletions;
        }
    }
}
