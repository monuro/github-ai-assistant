#!/bin/bash
# GitHub AI Assistant 一键安装脚本

set -e

echo "🚀 GitHub AI Assistant 安装程序"
echo "================================"

# 检测操作系统
OS="$(uname -s)"
ARCH="$(uname -m)"

# 安装目录
INSTALL_DIR="$HOME/.gh-ai"
BIN_DIR="$HOME/.local/bin"

# 颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

success() { echo -e "${GREEN}✓${NC} $1"; }
warn() { echo -e "${YELLOW}⚠${NC} $1"; }
error() { echo -e "${RED}✗${NC} $1"; exit 1; }

# 检查 Java 版本
check_java() {
    echo ""
    echo "📋 检查环境..."
    
    if command -v java &> /dev/null; then
        JAVA_VER=$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}' | cut -d'.' -f1)
        if [ "$JAVA_VER" -ge 21 ] 2>/dev/null; then
            success "Java $JAVA_VER 已安装"
            return 0
        fi
    fi
    
    warn "需要 Java 21+，正在查找..."
    
    # macOS: 查找 Temurin/Zulu/Oracle JDK 21+
    if [ "$OS" = "Darwin" ]; then
        for jdk in /Library/Java/JavaVirtualMachines/*/Contents/Home "$HOME/Library/Java/JavaVirtualMachines/"*/Contents/Home; do
            if [ -x "$jdk/bin/java" ]; then
                ver=$("$jdk/bin/java" -version 2>&1 | head -n 1 | awk -F '"' '{print $2}' | cut -d'.' -f1)
                if [ "$ver" -ge 21 ] 2>/dev/null; then
                    JAVA_HOME="$jdk"
                    success "找到 Java $ver: $JAVA_HOME"
                    return 0
                fi
            fi
        done
    fi
    
    error "未找到 Java 21+。请先安装：
    
    macOS:   brew install --cask temurin@21
    Ubuntu:  sudo apt install openjdk-21-jdk
    Windows: https://adoptium.net/temurin/releases/"
}

# 下载最新版本
download_jar() {
    echo ""
    echo "📥 下载最新版本..."
    
    mkdir -p "$INSTALL_DIR"
    
    # 从 GitHub Releases 下载（如果有）
    LATEST_URL="https://github.com/JackyST0/github-ai-assistant/releases/latest/download/github-ai-assistant.jar"
    
    if curl -fsSL --head "$LATEST_URL" &>/dev/null; then
        curl -fsSL -o "$INSTALL_DIR/github-ai-assistant.jar" "$LATEST_URL"
        success "下载完成"
    else
        # 没有 Release，尝试从源码构建
        warn "未找到预编译版本，正在从源码构建..."
        
        if ! command -v mvn &> /dev/null; then
            error "需要 Maven 来构建项目。请先安装 Maven。"
        fi
        
        TEMP_DIR=$(mktemp -d)
        git clone --depth 1 https://github.com/JackyST0/github-ai-assistant.git "$TEMP_DIR"
        cd "$TEMP_DIR"
        mvn package -DskipTests -q
        cp target/github-ai-assistant-*.jar "$INSTALL_DIR/github-ai-assistant.jar"
        rm -rf "$TEMP_DIR"
        success "构建完成"
    fi
}

# 创建启动脚本
create_launcher() {
    echo ""
    echo "🔧 创建启动脚本..."
    
    mkdir -p "$BIN_DIR"
    
    # 创建 gh-ai 启动脚本
    cat > "$BIN_DIR/gh-ai" << 'SCRIPT'
#!/bin/bash
# GitHub AI Assistant Launcher

# 查找 Java 21+
find_java() {
    if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
        echo "$JAVA_HOME/bin/java"
        return
    fi
    
    for jdk in /Library/Java/JavaVirtualMachines/*/Contents/Home "$HOME/Library/Java/JavaVirtualMachines/"*/Contents/Home /usr/lib/jvm/java-21-*; do
        if [ -x "$jdk/bin/java" ]; then
            ver=$("$jdk/bin/java" -version 2>&1 | head -n 1 | awk -F '"' '{print $2}' | cut -d'.' -f1)
            if [ "$ver" -ge 21 ] 2>/dev/null; then
                echo "$jdk/bin/java"
                return
            fi
        fi
    done
    
    echo "java"
}

JAVA=$(find_java)
JAR="$HOME/.gh-ai/github-ai-assistant.jar"

exec "$JAVA" -jar "$JAR" "$@"
SCRIPT
    
    chmod +x "$BIN_DIR/gh-ai"
    success "启动脚本已创建: $BIN_DIR/gh-ai"
}

# 配置向导
setup_config() {
    echo ""
    echo "⚙️  配置向导"
    echo "─────────────────────────────────────"
    
    CONFIG_FILE="$HOME/.gh-ai/config"
    
    # AI API 配置
    echo ""
    echo "选择 AI 后端:"
    echo "  1) OpenAI / 兼容 API (推荐)"
    echo "  2) 本地 Ollama (免费)"
    echo ""
    read -p "请选择 [1]: " ai_choice
    ai_choice=${ai_choice:-1}
    
    if [ "$ai_choice" = "1" ]; then
        echo ""
        read -p "API Key: " api_key
        read -p "API Base URL [https://api.openai.com]: " base_url
        base_url=${base_url:-https://api.openai.com}
        read -p "模型名称 [gpt-4o-mini]: " model
        model=${model:-gpt-4o-mini}
        
        cat > "$CONFIG_FILE" << EOF
export OPENAI_API_KEY="$api_key"
export OPENAI_BASE_URL="$base_url"
export OPENAI_MODEL="$model"
EOF
    else
        cat > "$CONFIG_FILE" << EOF
export OLLAMA_BASE_URL="http://localhost:11434"
EOF
        warn "请确保 Ollama 已运行: ollama serve"
    fi
    
    # GitHub Token（可选）
    echo ""
    read -p "GitHub Token (可选，用于 PR 审查): " gh_token
    if [ -n "$gh_token" ]; then
        echo "export GITHUB_TOKEN=\"$gh_token\"" >> "$CONFIG_FILE"
    fi
    
    success "配置已保存到 $CONFIG_FILE"
    
    # 添加到 shell 配置
    SHELL_RC="$HOME/.$(basename $SHELL)rc"
    if ! grep -q "gh-ai/config" "$SHELL_RC" 2>/dev/null; then
        echo "" >> "$SHELL_RC"
        echo "# GitHub AI Assistant" >> "$SHELL_RC"
        echo "[ -f \"\$HOME/.gh-ai/config\" ] && source \"\$HOME/.gh-ai/config\"" >> "$SHELL_RC"
        echo "export PATH=\"\$HOME/.local/bin:\$PATH\"" >> "$SHELL_RC"
        success "已添加到 $SHELL_RC"
    fi
}

# 主流程
main() {
    check_java
    download_jar
    create_launcher
    setup_config
    
    echo ""
    echo "════════════════════════════════════════"
    echo -e "${GREEN}✅ 安装完成！${NC}"
    echo "════════════════════════════════════════"
    echo ""
    echo "请重新打开终端，或执行："
    echo "  source ~/.$(basename $SHELL)rc"
    echo ""
    echo "然后试试："
    echo "  gh-ai --help"
    echo "  gh-ai explain \"git rebase\""
    echo "  gh-ai commit"
    echo ""
}

main
