package com.github.ai.assistant.client;

import com.github.ai.assistant.config.AppConfig;
import com.github.ai.assistant.model.IssueClassification;
import com.github.ai.assistant.model.PullRequestInfo;
import com.github.ai.assistant.model.PullRequestInfo.FileChange;
import org.kohsuke.github.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * GitHub API 客户端服务
 * 
 * 封装 GitHub API 调用，支持：
 * - Pull Request 操作
 * - Issue 操作
 * - Repository 操作
 */
@Service
public class GitHubClientService {

    private GitHub github;
    private final AppConfig appConfig;
    private boolean initialized = false;
    private String initError = null;

    public GitHubClientService(AppConfig appConfig) {
        this.appConfig = appConfig;
        // 延迟初始化，不在构造函数中连接 GitHub
        tryInitialize();
    }

    /**
     * 尝试初始化 GitHub 连接
     */
    private void tryInitialize() {
        try {
            String token = appConfig.getGithub().getToken();
            if (token != null && !token.isBlank()) {
                this.github = new GitHubBuilder().withOAuthToken(token).build();
                this.initialized = true;
            } else {
                // 尝试使用环境变量
                String envToken = System.getenv("GITHUB_TOKEN");
                if (envToken != null && !envToken.isBlank()) {
                    this.github = new GitHubBuilder().withOAuthToken(envToken).build();
                    this.initialized = true;
                } else {
                    this.initError = "GitHub Token 未配置。请设置环境变量 GITHUB_TOKEN 或在 application.yml 中配置 app.github.token";
                }
            }
        } catch (IOException e) {
            this.initError = "GitHub 连接失败: " + e.getMessage();
        }
    }

    /**
     * 确保已初始化，否则抛出异常
     */
    private void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException(initError != null ? initError : "GitHub 客户端未初始化");
        }
    }

    /**
     * 获取 Pull Request 详细信息
     */
    public PullRequestInfo getPullRequest(String repoFullName, int prNumber) throws IOException {
        ensureInitialized();
        GHRepository repo = github.getRepository(repoFullName);
        GHPullRequest pr = repo.getPullRequest(prNumber);
        
        List<FileChange> files = pr.listFiles().toList().stream()
            .map(file -> new FileChange(
                file.getFilename(),
                file.getStatus(),
                file.getAdditions(),
                file.getDeletions(),
                file.getPatch()
            ))
            .collect(Collectors.toList());
        
        // 获取完整的 diff
        String diff = files.stream()
            .map(f -> "--- " + f.filename() + "\n" + (f.patch() != null ? f.patch() : ""))
            .collect(Collectors.joining("\n\n"));
        
        return new PullRequestInfo(
            pr.getNumber(),
            pr.getTitle(),
            pr.getBody(),
            pr.getUser().getLogin(),
            pr.getState().name(),
            pr.getBase().getRef(),
            pr.getHead().getRef(),
            pr.getCreatedAt().toInstant(),
            files,
            diff
        );
    }

    /**
     * 获取 Issue 详细信息
     */
    public GHIssue getIssue(String repoFullName, int issueNumber) throws IOException {
        ensureInitialized();
        GHRepository repo = github.getRepository(repoFullName);
        return repo.getIssue(issueNumber);
    }

    /**
     * 获取仓库所有 Open Issues
     */
    public List<GHIssue> getOpenIssues(String repoFullName) throws IOException {
        ensureInitialized();
        GHRepository repo = github.getRepository(repoFullName);
        return repo.getIssues(GHIssueState.OPEN).stream()
            .filter(issue -> !issue.isPullRequest())  // 排除 PR
            .collect(Collectors.toList());
    }

    /**
     * 在 PR 上发布 Review Comment
     */
    public void createPullRequestReview(String repoFullName, int prNumber, String body, GHPullRequestReviewEvent event) 
            throws IOException {
        ensureInitialized();
        GHRepository repo = github.getRepository(repoFullName);
        GHPullRequest pr = repo.getPullRequest(prNumber);
        
        pr.createReview()
            .body(body)
            .event(event)
            .create();
    }

    /**
     * 在 Issue 上发布评论
     */
    public void createIssueComment(String repoFullName, int issueNumber, String body) throws IOException {
        ensureInitialized();
        GHRepository repo = github.getRepository(repoFullName);
        GHIssue issue = repo.getIssue(issueNumber);
        issue.comment(body);
    }

    /**
     * 为 Issue 添加标签
     */
    public void addLabelsToIssue(String repoFullName, int issueNumber, List<String> labels) throws IOException {
        ensureInitialized();
        GHRepository repo = github.getRepository(repoFullName);
        GHIssue issue = repo.getIssue(issueNumber);
        issue.addLabels(labels.toArray(new String[0]));
    }

    /**
     * 获取当前用户信息
     */
    public GHMyself getCurrentUser() throws IOException {
        ensureInitialized();
        return github.getMyself();
    }

    /**
     * 检查 API 连接状态
     */
    public boolean isConnected() {
        if (!initialized) {
            return false;
        }
        try {
            github.getMyself();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
