package com.github.ai.assistant.cli;

import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * 主命令入口
 * 
 * 使用方式：
 *   gh-ai commit     - 生成 commit message
 *   gh-ai review     - PR 代码审查
 *   gh-ai explain    - 解释代码或命令
 *   gh-ai issue      - Issue 管理
 */
@Component
@Command(
    name = "gh-ai",
    description = "AI-powered GitHub Assistant - 智能 GitHub 助手",
    mixinStandardHelpOptions = true,
    version = "0.1.0",
    subcommands = {
        CommitCommand.class,
        ReviewCommand.class,
        ExplainCommand.class,
        IssueCommand.class
    }
)
public class MainCommand implements Runnable {

    @Option(names = {"-v", "--verbose"}, description = "显示详细输出")
    private boolean verbose;

    @Override
    public void run() {
        // 检查配置状态
        boolean hasApiKey = System.getenv("OPENAI_API_KEY") != null && !System.getenv("OPENAI_API_KEY").isBlank();
        boolean hasGithubToken = System.getenv("GITHUB_TOKEN") != null && !System.getenv("GITHUB_TOKEN").isBlank();
        
        System.out.println("""
            
            🤖 GitHub AI Assistant v0.1.0
            ═════════════════════════════════════════
            
            AI 驱动的 GitHub 智能助手
            """);
        
        // 显示配置状态
        System.out.println("📋 配置状态：");
        System.out.println("   AI API Key:    " + (hasApiKey ? "✅ 已配置" : "❌ 未配置"));
        System.out.println("   GitHub Token:  " + (hasGithubToken ? "✅ 已配置" : "⚠️  未配置 (PR/Issue 功能需要)"));
        
        if (!hasApiKey) {
            System.out.println("""
            
            ⚠️  首次使用请配置 AI API：
            
               export OPENAI_API_KEY=your_api_key
               export OPENAI_BASE_URL=https://api.openai.com  # 可选
               export OPENAI_MODEL=gpt-4o-mini                # 可选
            """);
        }
        
        System.out.println("""
            
            📖 可用命令：
               commit   - 根据代码变更生成 commit message
               review   - AI 审查 Pull Request
               explain  - 解释代码或 Git 命令
               issue    - Issue 智能管理
            
            🚀 快速开始：
               gh-ai explain "git rebase -i"   # 解释 git 命令
               gh-ai commit                    # 生成 commit message
               gh-ai review --repo owner/repo --pr 123
            
            💡 使用 'gh-ai <command> --help' 查看详细帮助
            ═════════════════════════════════════════
            """);
    }
}
