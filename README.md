# GitHub AI Assistant 🤖

<p align="center">
  <strong>AI 驱动的 GitHub 命令行助手，让开发效率飞起来 🚀</strong>
</p>

<p align="center">
  <a href="https://github.com/JackyST0/github-ai-assistant/stargazers"><img src="https://img.shields.io/github/stars/JackyST0/github-ai-assistant?style=social" alt="Stars"></a>
  <a href="https://github.com/JackyST0/github-ai-assistant/blob/main/LICENSE"><img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="License"></a>
  <a href="https://openjdk.java.net/"><img src="https://img.shields.io/badge/Java-21+-orange.svg" alt="Java"></a>
  <a href="https://spring.io/projects/spring-boot"><img src="https://img.shields.io/badge/Spring%20Boot-3.3-green.svg" alt="Spring Boot"></a>
  <a href="https://spring.io/projects/spring-ai"><img src="https://img.shields.io/badge/Spring%20AI-1.0-blue.svg" alt="Spring AI"></a>
</p>

<p align="center">
  <a href="#-功能演示">功能演示</a> •
  <a href="#-快速开始">快速开始</a> •
  <a href="#-使用方法">使用方法</a> •
  <a href="#-支持的-ai-模型">支持的模型</a>
</p>

---

## 🎬 功能演示

### 智能生成 Commit Message

```bash
$ gh-ai commit

📊 变更统计
──────────────────────────────────────────────────
   文件数: 3 | 新增: +156 | 删除: -23
──────────────────────────────────────────────────
⠸ 🤖 AI 正在分析代码变更...

📝 生成的 Commit Message：
──────────────────────────────────────────────────
feat(auth): 添加用户登录验证功能

实现基于 JWT 的用户认证机制，支持 token 自动刷新
──────────────────────────────────────────────────

是否使用此 message 进行 commit? [Y/n] y
✅ Commit 成功！
   Commit Hash: a1b2c3d
```

### 智能生成 .gitignore

```bash
$ gh-ai ignore --dry-run

🔍 分析项目结构...

📋 检测结果：
──────────────────────────────────────────────────
   项目类型: Java
   构建工具: Maven
   IDE/编辑器: IntelliJ IDEA, VS Code
   框架: Spring Boot
──────────────────────────────────────────────────
⠸ 🤖 AI 正在生成 .gitignore...

📄 生成的 .gitignore：
──────────────────────────────────────────────────
# 系统文件
.DS_Store
Thumbs.db

# IDE
.idea/
*.iml
.vscode/

# Maven
target/
...
```

### 解释 Git 命令

```bash
$ gh-ai explain "git rebase -i HEAD~3"

📖 命令解释：

git rebase -i HEAD~3 是一个交互式变基命令，用于修改最近 3 次提交。

你可以：
• 重新排序提交
• 合并多个提交 (squash)
• 修改提交信息 (reword)
• 删除某个提交 (drop)

⚠️ 注意：不要对已推送的提交使用此命令
```

---

## ✨ 为什么选择 gh-ai？

| 痛点 | gh-ai 解决方案 |
|------|---------------|
| 😫 写 commit message 绞尽脑汁 | 🤖 AI 分析代码变更，一键生成规范 message |
| 😵 新项目忘记配置 .gitignore | 🔍 自动检测项目类型，生成完整忽略规则 |
| 🤔 Git 命令记不住 | 📖 自然语言解释任意 Git 命令 |
| 😰 Code Review 费时费力 | 🔍 AI 辅助审查，快速发现潜在问题 |
| 📝 写 README 不知从何下手 | 📄 分析项目结构，自动生成专业文档 |

---

## 🚀 功能特性

- 📝 **Commit Message 生成** - 分析 git diff，生成 Conventional Commits 格式的提交信息
- 🔍 **PR 智能审查** - AI 分析代码变更，发现 bug、安全问题、性能问题
- 📖 **命令/代码解释** - 用自然语言解释复杂的 Git 命令或代码片段
- 💬 **Issue 智能管理** - 自动分类 Issue、生成回复建议
- 🚫 **智能 .gitignore** - 检测项目类型，生成合适的忽略规则
- 📄 **智能 README** - 分析项目结构，生成专业的项目文档

---

## 📦 快速开始

### 前置条件

- Java 21+
- Maven 3.8+
- OpenAI API Key（或兼容的 API）

### 安装

```bash
# 1. 克隆项目
git clone https://github.com/JackyST0/github-ai-assistant.git
cd github-ai-assistant

# 2. 编译
mvn clean package -DskipTests

# 3. 配置环境变量（添加到 ~/.zshrc 或 ~/.bashrc）
export OPENAI_API_KEY=your_api_key
export OPENAI_BASE_URL=https://api.openai.com  # 可选，支持第三方 API
export OPENAI_MODEL=gpt-4o-mini                # 可选

# 4. 创建别名
alias gh-ai='java -jar ~/github-ai-assistant/target/github-ai-assistant-0.1.0.jar'

# 5. 验证安装
source ~/.zshrc && gh-ai --help
```

---

## 📖 使用方法

### 生成 Commit Message

```bash
gh-ai commit              # 分析 staged 变更，生成 commit message
gh-ai commit -y           # 跳过确认，直接提交
gh-ai commit --dry-run    # 仅预览，不执行提交
gh-ai commit -l en        # 生成英文 message
```

### PR 智能审查

```bash
gh-ai review --pr 123 --repo owner/repo           # 审查 PR
gh-ai review --pr 123 --repo owner/repo --focus security  # 聚焦安全问题
gh-ai review --pr 123 --repo owner/repo --comment # 自动发布评论
```

### 解释命令/代码

```bash
gh-ai explain "git rebase -i HEAD~3"      # 解释 Git 命令
gh-ai explain -f src/Example.java         # 解释代码文件
gh-ai explain "docker run -p 80:80 nginx" # 解释任意命令
```

### Issue 管理

```bash
gh-ai issue --id 456 --repo owner/repo --action classify  # 分类
gh-ai issue --id 456 --repo owner/repo --action suggest   # 生成回复
gh-ai issue --repo owner/repo --action summarize          # 汇总所有 Issue
```

### 智能生成 .gitignore

```bash
gh-ai ignore              # 分析项目，生成 .gitignore
gh-ai ignore --dry-run    # 仅预览
gh-ai ignore --append     # 追加到现有文件
```

### 智能生成 README

```bash
gh-ai readme              # 分析项目，生成 README.md
gh-ai readme --dry-run    # 仅预览
gh-ai readme -l en        # 生成英文版
```

---

## 🎯 支持的 AI 模型

支持任何 OpenAI 兼容的 API：

| 提供商 | 模型示例 |
|--------|---------|
| OpenAI | `gpt-4o`, `gpt-4o-mini`, `gpt-3.5-turbo` |
| Claude | `claude-sonnet-4-5`, `claude-haiku-4-5` |
| DeepSeek | `deepseek-chat`, `deepseek-v3` |
| 智谱 GLM | `glm-4.5-air`, `GLM-4-Flash` |
| 通义千问 | `qwen-plus`, `qwen-turbo` |
| 本地 Ollama | `llama3`, `qwen2`, `codellama` |

配置方式：
```bash
export OPENAI_BASE_URL=https://your-api-endpoint
export OPENAI_API_KEY=your-api-key
export OPENAI_MODEL=your-model-name
```

---

## 🛠 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 21+ | Virtual Threads、Record、Pattern Matching |
| Spring Boot | 3.3+ | 应用框架 |
| Spring AI | 1.0+ | AI 集成框架 |
| Picocli | 4.7+ | CLI 框架 |
| GitHub API | - | hub4j/github-api |

---

## 🏗 项目结构

```
github-ai-assistant/
├── src/main/java/com/github/ai/assistant/
│   ├── cli/           # CLI 命令 (CommitCommand, ReviewCommand, ...)
│   ├── service/       # 业务服务 (AIService, CommitService, ...)
│   ├── client/        # 外部客户端 (GitHubClientService)
│   ├── model/         # 数据模型
│   ├── config/        # 配置类
│   └── util/          # 工具类
├── src/main/resources/
│   └── application.yml
└── pom.xml
```

---

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

---

## 📄 License

[MIT License](LICENSE)

---

<p align="center">
  如果这个项目对你有帮助，请给个 ⭐️ 支持一下！
</p>
