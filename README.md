# GitHub AI Assistant 🤖

[![Java](https://img.shields.io/badge/Java-21+-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-green.svg)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0-blue.svg)](https://spring.io/projects/spring-ai)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

一个 AI 驱动的 GitHub 智能助手，帮助开发者提高工作效率。

## ✨ 功能特性

- 🔍 **PR 智能审查** - AI 分析代码变更，发现潜在问题
- 📝 **Commit Message 生成** - 根据代码差异自动生成规范的提交信息
- 💬 **Issue 智能管理** - 自动分类、生成回复建议、汇总分析
- 📖 **代码/命令解释** - 解释 Git 命令或代码片段

## 🛠 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 21+ | 使用 Virtual Threads、Record、Pattern Matching |
| Spring Boot | 3.3+ | 应用框架 |
| Spring AI | 1.0+ | AI 集成框架 |
| Picocli | 4.7+ | CLI 框架 |
| GitHub API | - | hub4j/github-api |

## 📦 快速开始

### 方式一：一键安装（推荐）

```bash
curl -fsSL https://raw.githubusercontent.com/JackyST0/github-ai-assistant/main/install.sh | bash
```

安装脚本会：
- ✅ 自动检测 Java 环境
- ✅ 下载最新版本
- ✅ 引导配置 API Key
- ✅ 创建 `gh-ai` 命令

### 方式二：手动安装

<details>
<summary>点击展开</summary>

**前置条件：**
- Java 21+
- Maven 3.8+

```bash
# 克隆项目
git clone https://github.com/JackyST0/github-ai-assistant.git
cd github-ai-assistant

# 编译
mvn clean package -DskipTests

# 运行
java -jar target/github-ai-assistant-0.1.0-SNAPSHOT.jar --help
```

**配置环境变量：**

```bash
export OPENAI_API_KEY=your_openai_api_key
export OPENAI_BASE_URL=https://api.openai.com  # 可选，支持代理
export OPENAI_MODEL=gpt-4o-mini                # 可选
export GITHUB_TOKEN=your_github_token          # 用于 PR 审查
```
</details>

## 📖 使用方法

### 生成 Commit Message

```bash
# 在 Git 仓库目录下
gh-ai commit

# 指定语言
gh-ai commit --lang en

# 使用本地模型
gh-ai commit --model ollama

# 仅生成不执行
gh-ai commit --dry-run

# 跳过确认直接提交
gh-ai commit -y

# 显示将要提交的文件列表
gh-ai commit --show-files
```

### PR 智能审查

```bash
# 审查指定 PR
gh-ai review --pr 123 --repo owner/repo

# 指定审查重点
gh-ai review --pr 123 --repo owner/repo --focus security

# 自动发布评论
gh-ai review --pr 123 --repo owner/repo --comment
```

### 解释命令/代码

```bash
# 解释 Git 命令
gh-ai explain "git rebase -i HEAD~3"

# 解释代码文件
gh-ai explain -f src/main/java/Example.java

# 简洁模式
gh-ai explain "git stash" --detail simple
```

### Issue 管理

```bash
# 分类 Issue
gh-ai issue --id 456 --repo owner/repo --action classify

# 生成回复建议
gh-ai issue --id 456 --repo owner/repo --action suggest

# 汇总所有 Open Issues
gh-ai issue --repo owner/repo --action summarize
```

## 🏗 项目结构

```
github-ai-assistant/
├── src/main/java/com/github/ai/assistant/
│   ├── GithubAiAssistantApplication.java   # 主入口
│   ├── cli/                                 # CLI 命令
│   │   ├── MainCommand.java
│   │   ├── CommitCommand.java
│   │   ├── ReviewCommand.java
│   │   ├── ExplainCommand.java
│   │   └── IssueCommand.java
│   ├── service/                             # 业务服务
│   │   ├── AIService.java
│   │   ├── CommitService.java
│   │   ├── ReviewService.java
│   │   ├── ExplainService.java
│   │   └── IssueService.java
│   ├── client/                              # 外部客户端
│   │   └── GitHubClientService.java
│   ├── model/                               # 数据模型
│   │   ├── ReviewResult.java
│   │   ├── IssueClassification.java
│   │   └── PullRequestInfo.java
│   ├── config/                              # 配置
│   │   ├── AppConfig.java
│   │   └── AIConfig.java
│   └── util/                                # 工具类
│       └── ConsoleUtils.java
├── src/main/resources/
│   ├── application.yml                      # 主配置
│   └── application-dev.yml                  # 开发配置
└── src/test/java/com/github/ai/assistant/   # 单元测试
    ├── service/
    │   ├── CommitServiceTest.java
    │   ├── ReviewServiceTest.java
    │   ├── ExplainServiceTest.java
    │   └── IssueServiceTest.java
    └── util/
        └── ConsoleUtilsTest.java
```

## 🔧 配置选项

| 配置项 | 环境变量 | 默认值 | 说明 |
|--------|---------|--------|------|
| `app.github.token` | `GITHUB_TOKEN` | - | GitHub Personal Access Token |
| `spring.ai.openai.api-key` | `OPENAI_API_KEY` | - | OpenAI API Key |
| `spring.ai.openai.base-url` | `OPENAI_BASE_URL` | `https://api.openai.com` | OpenAI API 地址 |
| `spring.ai.ollama.base-url` | `OLLAMA_BASE_URL` | `http://localhost:11434` | Ollama 服务地址 |
| `app.ai.default-model` | - | `openai` | 默认 AI 模型 |
| `app.ai.default-language` | - | `zh` | 默认语言 |

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 License

MIT License - 详见 [LICENSE](LICENSE) 文件

## 🎯 支持的 AI 模型

本项目支持任何 OpenAI 兼容的 API，包括：

| 模型 | 配置 |
|------|------|
| OpenAI GPT | `gpt-4o`, `gpt-4o-mini`, `gpt-3.5-turbo` |
| Claude | `claude-sonnet-4-5`, `claude-haiku-4-5` |
| DeepSeek | `deepseek-chat`, `deepseek-v3-0324-turbo` |
| 智谱 GLM | `glm-4.5-air`, `GLM-4-Flash` |
| 通义千问 | `qwen-plus-latest`, `qwen-turbo-latest` |
| 本地 Ollama | `llama3`, `qwen2`, `codellama` |

通过环境变量配置：
```bash
export OPENAI_BASE_URL=https://your-api-endpoint
export OPENAI_API_KEY=your-api-key
export OPENAI_MODEL=your-model-name
```

## 🔮 Roadmap

- [ ] 支持 GitHub Actions 集成
- [ ] 支持 MCP 协议 (Model Context Protocol)
- [ ] GraalVM Native Image 支持（更快启动）
- [ ] 交互式 TUI 界面
- [ ] Homebrew/Scoop 安装支持
- [ ] 支持更多代码托管平台 (GitLab, Gitee)
