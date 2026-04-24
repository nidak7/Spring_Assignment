package com.viral.gateway.controller;

import com.viral.gateway.dto.request.CreateCommentRequest;
import com.viral.gateway.dto.request.CreatePostRequest;
import com.viral.gateway.dto.request.LikePostRequest;
import com.viral.gateway.dto.response.CommentResponse;
import com.viral.gateway.dto.response.LikePostResponse;
import com.viral.gateway.dto.response.PostResponse;
import com.viral.gateway.service.PostService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getPost(@PathVariable Long postId) {
        return ResponseEntity.ok(postService.getPost(postId));
    }

    @PostMapping
    public ResponseEntity<PostResponse> createPost(@Valid @RequestBody CreatePostRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.createPost(request));
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<CommentResponse> createComment(@PathVariable Long postId,
                                                         @Valid @RequestBody CreateCommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.createComment(postId, request));
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<LikePostResponse> likePost(@PathVariable Long postId,
                                                     @Valid @RequestBody LikePostRequest request) {
        return ResponseEntity.ok(postService.likePost(postId, request));
    }
}
