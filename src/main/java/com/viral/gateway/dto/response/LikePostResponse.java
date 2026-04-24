package com.viral.gateway.dto.response;

public class LikePostResponse {

    private Long postId;
    private Long userId;
    private Long viralityScore;
    private String message;

    public LikePostResponse() {
    }

    public LikePostResponse(Long postId, Long userId, Long viralityScore, String message) {
        this.postId = postId;
        this.userId = userId;
        this.viralityScore = viralityScore;
        this.message = message;
    }

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getViralityScore() {
        return viralityScore;
    }

    public void setViralityScore(Long viralityScore) {
        this.viralityScore = viralityScore;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
