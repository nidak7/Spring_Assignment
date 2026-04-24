package com.viral.gateway.dto.response;

import java.time.LocalDateTime;

public class CommentResponse {

    private Long id;
    private Long postId;
    private Long authorId;
    private Boolean isAuthorBot;
    private String content;
    private Long parentId;
    private Integer depthLevel;
    private LocalDateTime createdAt;
    private Long viralityScore;
    private Long botCount;

    public CommentResponse() {
    }

    public CommentResponse(Long id, Long postId, Long authorId, Boolean isAuthorBot, String content, Long parentId,
                           Integer depthLevel, LocalDateTime createdAt, Long viralityScore, Long botCount) {
        this.id = id;
        this.postId = postId;
        this.authorId = authorId;
        this.isAuthorBot = isAuthorBot;
        this.content = content;
        this.parentId = parentId;
        this.depthLevel = depthLevel;
        this.createdAt = createdAt;
        this.viralityScore = viralityScore;
        this.botCount = botCount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public Boolean getIsAuthorBot() {
        return isAuthorBot;
    }

    public void setIsAuthorBot(Boolean authorBot) {
        isAuthorBot = authorBot;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Integer getDepthLevel() {
        return depthLevel;
    }

    public void setDepthLevel(Integer depthLevel) {
        this.depthLevel = depthLevel;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getViralityScore() {
        return viralityScore;
    }

    public void setViralityScore(Long viralityScore) {
        this.viralityScore = viralityScore;
    }

    public Long getBotCount() {
        return botCount;
    }

    public void setBotCount(Long botCount) {
        this.botCount = botCount;
    }
}
