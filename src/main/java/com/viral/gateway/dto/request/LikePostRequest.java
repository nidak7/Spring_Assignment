package com.viral.gateway.dto.request;

import jakarta.validation.constraints.NotNull;

public class LikePostRequest {

    @NotNull
    private Long userId;

    public LikePostRequest() {
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
