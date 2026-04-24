package com.viral.gateway.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateCommentRequest {

    @NotNull
    private Long authorId;

    @NotNull
    private Boolean isAuthorBot;

    @NotBlank
    @Size(max = 2000)
    private String content;

    private Long parentId;

    public CreateCommentRequest() {
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
}
