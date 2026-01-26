package com.github.ai.assistant.cli;

import com.github.ai.assistant.service.ReadmeService;
import com.github.ai.assistant.service.ReadmeService.ProjectContext;
import com.github.ai.assistant.util.ConsoleUtils;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * README 生成命令
 * 
 * 分析项目结构，自动生成 README.md 文件
 */
@Component
@Command(
    name = "readme",
    description = "智能生成 README.md 文件",
    mixinStandardHelpOptions = true
)
public class ReadmeCommand implements Callable<Integer> {

    private final ReadmeService readmeService;

    @Option(names = {"-d", "--dir"}, description = "项目目录 (默认为当前目录)")
    private File directory = new File(".");

    @Option(names = {"-o", "--output"}, description = "输出文件名 (默认为 README.md)")
    private String outputFile = "README.md";

    @Option(names = {"-l", "--lang"}, description = "README 语言 (zh/en)", defaultValue = "zh")
    private String language;

    @Option(names = {"--dry-run"}, description = "仅预览不写入文件")
    private boolean dryRun;

    @Option(names = {"-y", "--yes"}, description = "跳过确认直接写入")
    private boolean autoConfirm;

    @Option(names = {"-m", "--model"}, description = "AI 模型 (openai/ollama)", defaultValue = "openai")
    private String model;

    public ReadmeCommand(ReadmeService readmeService) {
        this.readmeService = readmeService;
    }

    @Override
    public Integer call() {
        try {
            Path projectPath = directory.toPath().toAbsolutePath();
            
            if (!Files.isDirectory(projectPath)) {
                ConsoleUtils.error("目录不存在: " + projectPath);
                return 1;
            }

            // 分析项目
            System.out.println("\n🔍 分析项目结构...");
            ProjectContext context = readmeService.analyzeProject(projectPath);
            
            // 显示检测结果
            System.out.println("\n📋 检测结果：");
            ConsoleUtils.separator();
            System.out.println("   项目名称: " + context.projectName());
            System.out.println("   项目类型: " + String.join(", ", context.projectTypes()));
            if (context.description() != null && !context.description().isBlank()) {
                System.out.println("   项目描述: " + context.description());
            }
            if (!context.dependencies().isEmpty()) {
                System.out.println("   主要依赖: " + String.join(", ", context.dependencies().subList(0, Math.min(5, context.dependencies().size()))));
            }
            ConsoleUtils.separator();

            // 生成 README
            String readmeContent = ConsoleUtils.withSpinner("🤖 AI 正在生成 README...",
                () -> readmeService.generateReadme(context, language, model));

            // 显示生成内容
            System.out.println("\n📄 生成的 README：");
            ConsoleUtils.separator();
            System.out.println(readmeContent);
            ConsoleUtils.separator();

            // Dry-run 模式
            if (dryRun) {
                System.out.println("\n✨ [Dry Run] 未写入文件");
                System.out.println("💡 提示：移除 --dry-run 参数可写入文件");
                return 0;
            }

            // 检查是否存在现有文件
            Path readmePath = projectPath.resolve(outputFile);
            if (Files.exists(readmePath) && !autoConfirm) {
                ConsoleUtils.warn("检测到已存在 " + outputFile + " 文件");
                boolean shouldOverwrite = ConsoleUtils.confirm("是否覆盖现有文件?", false);
                if (!shouldOverwrite) {
                    ConsoleUtils.info("已取消");
                    return 0;
                }
            }

            // 确认写入
            boolean shouldWrite = autoConfirm || ConsoleUtils.confirm("\n是否写入到 " + outputFile + "?", true);

            if (shouldWrite) {
                Files.writeString(readmePath, readmeContent);
                ConsoleUtils.success(outputFile + " 已生成！");
            } else {
                ConsoleUtils.info("已取消");
            }

            return 0;
        } catch (Exception e) {
            ConsoleUtils.error("错误: " + e.getMessage());
            return 1;
        }
    }
}
