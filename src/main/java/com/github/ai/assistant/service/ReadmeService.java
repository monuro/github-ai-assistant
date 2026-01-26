package com.github.ai.assistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * README 生成服务
 * 
 * 分析项目结构并生成 README.md 文件
 */
@Service
public class ReadmeService {

    private final AIService aiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ReadmeService(AIService aiService) {
        this.aiService = aiService;
    }

    /**
     * 项目上下文
     */
    public record ProjectContext(
        String projectName,
        String description,
        List<String> projectTypes,
        List<String> dependencies,
        List<String> mainFiles,
        String structureTree
    ) {}

    /**
     * 分析项目结构
     */
    public ProjectContext analyzeProject(Path projectPath) throws IOException {
        String projectName = projectPath.getFileName().toString();
        String description = "";
        List<String> projectTypes = new ArrayList<>();
        List<String> dependencies = new ArrayList<>();
        List<String> mainFiles = new ArrayList<>();

        // 解析 pom.xml (Maven)
        Path pomPath = projectPath.resolve("pom.xml");
        if (Files.exists(pomPath)) {
            projectTypes.add("Java/Maven");
            String pomContent = Files.readString(pomPath);
            // 提取 artifactId 作为项目名
            if (pomContent.contains("<artifactId>")) {
                int start = pomContent.indexOf("<artifactId>") + 12;
                int end = pomContent.indexOf("</artifactId>", start);
                if (end > start) {
                    projectName = pomContent.substring(start, end);
                }
            }
            // 提取 description
            if (pomContent.contains("<description>")) {
                int start = pomContent.indexOf("<description>") + 13;
                int end = pomContent.indexOf("</description>", start);
                if (end > start) {
                    description = pomContent.substring(start, end);
                }
            }
            mainFiles.add("pom.xml");
        }

        // 解析 build.gradle (Gradle)
        if (Files.exists(projectPath.resolve("build.gradle")) || Files.exists(projectPath.resolve("build.gradle.kts"))) {
            if (!projectTypes.contains("Java/Maven")) projectTypes.add("Java/Gradle");
            mainFiles.add("build.gradle");
        }

        // 解析 package.json (Node.js)
        Path packageJsonPath = projectPath.resolve("package.json");
        if (Files.exists(packageJsonPath)) {
            projectTypes.add("Node.js");
            try {
                JsonNode root = objectMapper.readTree(Files.readString(packageJsonPath));
                if (root.has("name")) {
                    projectName = root.get("name").asText();
                }
                if (root.has("description")) {
                    description = root.get("description").asText();
                }
                // 提取依赖
                if (root.has("dependencies")) {
                    root.get("dependencies").fieldNames().forEachRemaining(dependencies::add);
                }
            } catch (Exception ignored) {}
            mainFiles.add("package.json");
        }

        // 解析 requirements.txt / pyproject.toml (Python)
        if (Files.exists(projectPath.resolve("requirements.txt"))) {
            projectTypes.add("Python");
            try {
                List<String> lines = Files.readAllLines(projectPath.resolve("requirements.txt"));
                for (String line : lines) {
                    if (!line.isBlank() && !line.startsWith("#")) {
                        String dep = line.split("[=<>]")[0].trim();
                        if (!dep.isEmpty()) dependencies.add(dep);
                    }
                }
            } catch (Exception ignored) {}
            mainFiles.add("requirements.txt");
        }
        if (Files.exists(projectPath.resolve("pyproject.toml"))) {
            if (!projectTypes.contains("Python")) projectTypes.add("Python");
            mainFiles.add("pyproject.toml");
        }

        // 检测 Go
        if (Files.exists(projectPath.resolve("go.mod"))) {
            projectTypes.add("Go");
            mainFiles.add("go.mod");
        }

        // 检测 Rust
        if (Files.exists(projectPath.resolve("Cargo.toml"))) {
            projectTypes.add("Rust");
            mainFiles.add("Cargo.toml");
        }

        // 生成项目结构树
        String structureTree = generateStructureTree(projectPath, 3);

        // 默认值
        if (projectTypes.isEmpty()) projectTypes.add("Unknown");

        return new ProjectContext(projectName, description, projectTypes, dependencies, mainFiles, structureTree);
    }

    /**
     * 生成项目结构树
     */
    private String generateStructureTree(Path projectPath, int maxDepth) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(projectPath.getFileName().toString()).append("/\n");
        generateTree(projectPath, sb, "", maxDepth, 0);
        return sb.toString();
    }

    private void generateTree(Path dir, StringBuilder sb, String prefix, int maxDepth, int currentDepth) throws IOException {
        if (currentDepth >= maxDepth) return;
        
        List<Path> entries;
        try (Stream<Path> stream = Files.list(dir)) {
            entries = stream
                .filter(p -> !p.getFileName().toString().startsWith("."))
                .filter(p -> !p.getFileName().toString().equals("node_modules"))
                .filter(p -> !p.getFileName().toString().equals("target"))
                .filter(p -> !p.getFileName().toString().equals("build"))
                .filter(p -> !p.getFileName().toString().equals("dist"))
                .filter(p -> !p.getFileName().toString().equals("__pycache__"))
                .sorted()
                .toList();
        }

        for (int i = 0; i < entries.size(); i++) {
            Path entry = entries.get(i);
            boolean isLast = (i == entries.size() - 1);
            String connector = isLast ? "└── " : "├── ";
            String childPrefix = isLast ? "    " : "│   ";
            
            sb.append(prefix).append(connector).append(entry.getFileName().toString());
            if (Files.isDirectory(entry)) {
                sb.append("/");
            }
            sb.append("\n");
            
            if (Files.isDirectory(entry)) {
                generateTree(entry, sb, prefix + childPrefix, maxDepth, currentDepth + 1);
            }
        }
    }

    /**
     * 生成 README 内容
     */
    public String generateReadme(ProjectContext context, String language, String model) {
        String systemPrompt = buildSystemPrompt(language);
        String userMessage = buildUserMessage(context);
        
        return aiService.chat(model, systemPrompt, userMessage);
    }

    private String buildSystemPrompt(String language) {
        boolean isChinese = language.equals("zh");
        
        if (isChinese) {
            return """
                你是一个专业的技术文档撰写专家，擅长为开源项目编写清晰、专业的 README.md 文件。
                
                生成的 README 应该包含以下部分（根据项目实际情况选择）：
                1. 项目标题和简介
                2. 功能特性
                3. 技术栈（如果能检测到）
                4. 快速开始 / 安装说明
                5. 使用方法
                6. 项目结构（简化版）
                7. 贡献指南（简短）
                8. 许可证
                
                规则：
                - 使用中文撰写
                - 使用 Markdown 格式
                - 适当使用 emoji 增加可读性
                - 根据项目类型提供对应的安装和运行命令
                - 保持简洁专业，避免废话
                - 只输出 README 内容，不要其他解释
                """;
        } else {
            return """
                You are a professional technical documentation expert skilled at writing clear, professional README.md files for open source projects.
                
                The generated README should include the following sections (as applicable):
                1. Project title and description
                2. Features
                3. Tech stack (if detectable)
                4. Quick start / Installation
                5. Usage
                6. Project structure (simplified)
                7. Contributing (brief)
                8. License
                
                Rules:
                - Write in English
                - Use Markdown format
                - Use emojis appropriately for readability
                - Provide installation and run commands based on project type
                - Keep it concise and professional
                - Only output README content, no explanations
                """;
        }
    }

    private String buildUserMessage(ProjectContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("请根据以下项目信息生成 README.md 文件：\n\n");
        sb.append("项目名称: ").append(context.projectName()).append("\n");
        sb.append("项目类型: ").append(String.join(", ", context.projectTypes())).append("\n");
        
        if (context.description() != null && !context.description().isBlank()) {
            sb.append("项目描述: ").append(context.description()).append("\n");
        }
        
        if (!context.dependencies().isEmpty()) {
            sb.append("主要依赖: ").append(String.join(", ", context.dependencies())).append("\n");
        }
        
        sb.append("\n项目结构：\n```\n").append(context.structureTree()).append("```\n");

        return sb.toString();
    }
}
