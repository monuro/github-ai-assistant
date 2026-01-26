package com.github.ai.assistant.cli;

import com.github.ai.assistant.service.ReviewService;
import com.github.ai.assistant.util.ConsoleUtils;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

/**
 * PR 审查命令
 * 
 * 使用 AI 审查 GitHub Pull Request
 */
@Component
@Command(
    name = "review",
    description = "AI 智能审查 Pull Request",
    mixinStandardHelpOptions = true
)
public class ReviewCommand implements Callable<Integer> {

    private final ReviewService reviewService;

    @Option(names = {"--pr"}, description = "PR 编号", required = true)
    private int prNumber;

    @Option(names = {"-r", "--repo"}, description = "仓库名 (格式: owner/repo)")
    private String repository;

    @Option(names = {"--comment"}, description = "自动发布评论到 GitHub", defaultValue = "false")
    private boolean autoComment;

    @Option(names = {"-m", "--model"}, description = "AI 模型 (openai/ollama)", defaultValue = "openai")
    private String model;

    @Option(names = {"--focus"}, description = "审查重点 (security/performance/style/all)", defaultValue = "all")
    private String focus;

    public ReviewCommand(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @Override
    public Integer call() {
        try {
            var result = ConsoleUtils.withSpinner("🔍 正在审查 PR #" + prNumber + "...",
                () -> reviewService.reviewPullRequest(repository, prNumber, focus, model));
            
            System.out.println("📊 PR 审查结果");
            System.out.println("═".repeat(50));
            System.out.println(result.summary());
            System.out.println();
            
            if (!result.issues().isEmpty()) {
                System.out.println("⚠️ 发现的问题：");
                result.issues().forEach(issue -> 
                    System.out.println("  • " + issue)
                );
                System.out.println();
            }
            
            if (!result.suggestions().isEmpty()) {
                System.out.println("💡 改进建议：");
                result.suggestions().forEach(suggestion -> 
                    System.out.println("  • " + suggestion)
                );
            }
            
            System.out.println("═".repeat(50));
            System.out.println("评分: " + result.score() + "/100");
            
            if (autoComment) {
                System.out.println("\n📤 正在发布评论到 GitHub...");
                reviewService.postReviewComment(repository, prNumber, result);
                System.out.println("✅ 评论已发布");
            }
            
            return 0;
        } catch (Exception e) {
            System.err.println("❌ 错误: " + e.getMessage());
            return 1;
        }
    }
}
