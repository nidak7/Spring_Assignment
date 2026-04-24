package com.viral.gateway.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "comments",
        indexes = {
                @Index(name = "idx_comments_post_id", columnList = "post_id"),
                @Index(name = "idx_comments_parent_id", columnList = "parent_id")
        }
)
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "is_author_bot", nullable = false)
    private Boolean isAuthorBot;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "depth_level", nullable = false)
    private Integer depthLevel;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Comment() {
    }

    public Comment(Long postId, Long authorId, Boolean isAuthorBot, String content, Long parentId, Integer depthLevel) {
        this.postId = postId;
        this.authorId = authorId;
        this.isAuthorBot = isAuthorBot;
        this.content = content;
        this.parentId = parentId;
        this.depthLevel = depthLevel;
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
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
}
