package com.viral.gateway.dto.response;

import java.time.LocalDateTime;

public class PostResponse {

    private Long id;
    private Long authorId;
    private Boolean isAuthorBot;
    private String content;
    private LocalDateTime createdAt;
    private Long viralityScore;

    public PostResponse() {
    }

    public PostResponse(Long id, Long authorId, Boolean isAuthorBot, String content, LocalDateTime createdAt, Long viralityScore) {
        this.id = id;
        this.authorId = authorId;
        this.isAuthorBot = isAuthorBot;
        this.content = content;
        this.createdAt = createdAt;
        this.viralityScore = viralityScore;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
}
