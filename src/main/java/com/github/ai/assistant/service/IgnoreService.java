package com.github.ai.assistant.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 智能 .gitignore 生成服务
 * 
 * 分析项目结构并生成合适的 .gitignore 文件
 */
@Service
public class IgnoreService {

    private final AIService aiService;

    public IgnoreService(AIService aiService) {
        this.aiService = aiService;
    }

    /**
     * 项目信息
     */
    public record ProjectInfo(
        List<String> projectTypes,
        List<String> buildTools,
        List<String> ides,
        List<String> frameworks,
        List<String> detectedFiles
    ) {}

    /**
     * 分析项目结构
     */
    public ProjectInfo analyzeProject(Path projectPath) throws IOException {
        List<String> projectTypes = new ArrayList<>();
        List<String> buildTools = new ArrayList<>();
        List<String> ides = new ArrayList<>();
        List<String> frameworks = new ArrayList<>();
        List<String> detectedFiles = new ArrayList<>();

        // 检测 Java/Maven
        if (Files.exists(projectPath.resolve("pom.xml"))) {
            projectTypes.add("Java");
            buildTools.add("Maven");
            detectedFiles.add("pom.xml");
        }

        // 检测 Java/Gradle
        if (Files.exists(projectPath.resolve("build.gradle")) || Files.exists(projectPath.resolve("build.gradle.kts"))) {
            if (!projectTypes.contains("Java")) projectTypes.add("Java");
            buildTools.add("Gradle");
            detectedFiles.add("build.gradle");
        }

        // 检测 Node.js
        if (Files.exists(projectPath.resolve("package.json"))) {
            projectTypes.add("Node.js");
            buildTools.add("npm/yarn");
            detectedFiles.add("package.json");
            
            // 检测 Node.js 框架
            String packageJson = Files.readString(projectPath.resolve("package.json"));
            if (packageJson.contains("\"react\"")) frameworks.add("React");
            if (packageJson.contains("\"vue\"")) frameworks.add("Vue");
            if (packageJson.contains("\"next\"")) frameworks.add("Next.js");
            if (packageJson.contains("\"express\"")) frameworks.add("Express");
            if (packageJson.contains("\"typescript\"")) projectTypes.add("TypeScript");
        }

        // 检测 Python
        if (Files.exists(projectPath.resolve("requirements.txt"))) {
            projectTypes.add("Python");
            buildTools.add("pip");
            detectedFiles.add("requirements.txt");
        }
        if (Files.exists(projectPath.resolve("pyproject.toml"))) {
            if (!projectTypes.contains("Python")) projectTypes.add("Python");
            buildTools.add("Poetry/PDM");
            detectedFiles.add("pyproject.toml");
        }
        if (Files.exists(projectPath.resolve("setup.py"))) {
            if (!projectTypes.contains("Python")) projectTypes.add("Python");
            detectedFiles.add("setup.py");
        }

        // 检测 Go
        if (Files.exists(projectPath.resolve("go.mod"))) {
            projectTypes.add("Go");
            buildTools.add("Go Modules");
            detectedFiles.add("go.mod");
        }

        // 检测 Rust
        if (Files.exists(projectPath.resolve("Cargo.toml"))) {
            projectTypes.add("Rust");
            buildTools.add("Cargo");
            detectedFiles.add("Cargo.toml");
        }

        // 检测 C/C++
        if (Files.exists(projectPath.resolve("CMakeLists.txt"))) {
            projectTypes.add("C/C++");
            buildTools.add("CMake");
            detectedFiles.add("CMakeLists.txt");
        }
        if (Files.exists(projectPath.resolve("Makefile"))) {
            if (!projectTypes.contains("C/C++")) projectTypes.add("C/C++");
            buildTools.add("Make");
            detectedFiles.add("Makefile");
        }

        // 检测 .NET
        if (hasFileWithExtension(projectPath, ".csproj") || hasFileWithExtension(projectPath, ".sln")) {
            projectTypes.add(".NET/C#");
            buildTools.add("MSBuild");
            detectedFiles.add("*.csproj/*.sln");
        }

        // 检测 Ruby
        if (Files.exists(projectPath.resolve("Gemfile"))) {
            projectTypes.add("Ruby");
            buildTools.add("Bundler");
            detectedFiles.add("Gemfile");
        }

        // 检测 PHP
        if (Files.exists(projectPath.resolve("composer.json"))) {
            projectTypes.add("PHP");
            buildTools.add("Composer");
            detectedFiles.add("composer.json");
        }

        // 检测 IDE
        if (Files.exists(projectPath.resolve(".idea"))) {
            ides.add("IntelliJ IDEA");
        }
        if (Files.exists(projectPath.resolve(".vscode"))) {
            ides.add("VS Code");
        }
        if (Files.exists(projectPath.resolve(".eclipse"))) {
            ides.add("Eclipse");
        }
        if (hasFileWithExtension(projectPath, ".xcodeproj") || hasFileWithExtension(projectPath, ".xcworkspace")) {
            ides.add("Xcode");
        }

        // 检测 Spring Boot
        if (projectTypes.contains("Java")) {
            Path srcMain = projectPath.resolve("src/main/java");
            if (Files.exists(srcMain)) {
                try (var stream = Files.walk(srcMain, 5)) {
                    boolean hasSpring = stream.anyMatch(p -> {
                        try {
                            return p.toString().endsWith(".java") && 
                                   Files.readString(p).contains("@SpringBootApplication");
                        } catch (IOException e) {
                            return false;
                        }
                    });
                    if (hasSpring) frameworks.add("Spring Boot");
                }
            }
        }

        // 默认值
        if (projectTypes.isEmpty()) projectTypes.add("Unknown");
        if (buildTools.isEmpty()) buildTools.add("Unknown");
        if (ides.isEmpty()) ides.add("Unknown");

        return new ProjectInfo(projectTypes, buildTools, ides, frameworks, detectedFiles);
    }

    /**
     * 检查目录下是否有指定扩展名的文件
     */
    private boolean hasFileWithExtension(Path dir, String extension) throws IOException {
        try (var stream = Files.list(dir)) {
            return stream.anyMatch(p -> p.toString().endsWith(extension));
        }
    }

    /**
     * 生成 .gitignore 内容
     */
    public String generateGitignore(ProjectInfo projectInfo, String existingContent, boolean append, String model) {
        String systemPrompt = buildSystemPrompt();
        String userMessage = buildUserMessage(projectInfo, existingContent, append);
        
        return aiService.chat(model, systemPrompt, userMessage);
    }

    private String buildSystemPrompt() {
        return """
            你是一个专业的软件工程师，擅长创建完善的 .gitignore 文件。
            
            规则：
            1. 根据检测到的项目类型、构建工具、IDE 生成合适的忽略规则
            2. 按类别分组，使用注释说明每个部分
            3. 包含常见的系统文件（.DS_Store, Thumbs.db 等）
            4. 包含常见的 IDE 文件和缓存
            5. 包含构建产物、依赖目录、日志文件等
            6. 只输出 .gitignore 内容，不要其他解释
            7. 使用中文注释
            """;
    }

    private String buildUserMessage(ProjectInfo projectInfo, String existingContent, boolean append) {
        StringBuilder sb = new StringBuilder();
        sb.append("请根据以下项目信息生成 .gitignore 文件：\n\n");
        sb.append("项目类型: ").append(String.join(", ", projectInfo.projectTypes())).append("\n");
        sb.append("构建工具: ").append(String.join(", ", projectInfo.buildTools())).append("\n");
        sb.append("IDE/编辑器: ").append(String.join(", ", projectInfo.ides())).append("\n");
        
        if (!projectInfo.frameworks().isEmpty()) {
            sb.append("框架: ").append(String.join(", ", projectInfo.frameworks())).append("\n");
        }

        if (append && existingContent != null && !existingContent.isBlank()) {
            sb.append("\n现有 .gitignore 内容（请不要重复这些规则）：\n");
            sb.append("```\n").append(existingContent).append("\n```\n");
            sb.append("\n请只生成需要补充的规则。");
        }

        return sb.toString();
    }
}
