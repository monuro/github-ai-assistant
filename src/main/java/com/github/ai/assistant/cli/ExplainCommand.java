package com.github.ai.assistant.cli;

import com.github.ai.assistant.service.ExplainService;
import com.github.ai.assistant.util.ConsoleUtils;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.util.concurrent.Callable;

/**
 * 代码/命令解释命令
 * 
 * 使用 AI 解释 Git 命令或代码片段
 */
@Component
@Command(
    name = "explain",
    description = "AI 解释 Git 命令或代码",
    mixinStandardHelpOptions = true
)
public class ExplainCommand implements Callable<Integer> {

    private final ExplainService explainService;

    @Parameters(index = "0", description = "要解释的命令或代码", arity = "0..1")
    private String input;

    @Option(names = {"-f", "--file"}, description = "要解释的代码文件")
    private File file;

    @Option(names = {"-l", "--lang"}, description = "输出语言 (zh/en)", defaultValue = "zh")
    private String language;

    @Option(names = {"-m", "--model"}, description = "AI 模型 (openai/ollama)", defaultValue = "openai")
    private String model;

    @Option(names = {"--detail"}, description = "详细程度 (simple/detailed)", defaultValue = "detailed")
    private String detailLevel;

    public ExplainCommand(ExplainService explainService) {
        this.explainService = explainService;
    }

    @Override
    public Integer call() {
        try {
            String content;
            String type;
            
            if (file != null) {
                content = java.nio.file.Files.readString(file.toPath());
                type = "code";
            } else if (input != null && !input.isBlank()) {
                content = input;
                type = input.startsWith("git ") ? "git-command" : "code";
            } else {
                System.err.println("❌ 请提供要解释的命令或使用 -f 指定文件");
                return 1;
            }
            
            final String finalContent = content;
            final String finalType = type;
            String explanation = ConsoleUtils.withSpinner("🤖 AI 正在思考...", 
                () -> explainService.explain(finalContent, finalType, language, detailLevel, model));
            
            String displayName = file != null ? file.getName() : input;
            System.out.println("📖 " + displayName);
            System.out.println("─".repeat(50));
            System.out.println(explanation);
            System.out.println("─".repeat(50));
            
            return 0;
        } catch (Exception e) {
            System.err.println("❌ 错误: " + e.getMessage());
            return 1;
        }
    }
}
