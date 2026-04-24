package com.viral.gateway.service;

import com.viral.gateway.dto.request.CreateCommentRequest;
import com.viral.gateway.dto.request.CreatePostRequest;
import com.viral.gateway.dto.request.LikePostRequest;
import com.viral.gateway.dto.response.CommentResponse;
import com.viral.gateway.dto.response.LikePostResponse;
import com.viral.gateway.dto.response.PostResponse;
import com.viral.gateway.entity.Comment;
import com.viral.gateway.entity.Post;
import com.viral.gateway.exception.BadRequestException;
import com.viral.gateway.exception.ResourceNotFoundException;
import com.viral.gateway.repository.BotRepository;
import com.viral.gateway.repository.CommentRepository;
import com.viral.gateway.repository.PostRepository;
import com.viral.gateway.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class PostService {

    private static final Logger log = LoggerFactory.getLogger(PostService.class);
    private static final int MAX_DEPTH_LEVEL = 20;

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final BotRepository botRepository;
    private final RedisGuardrailService redisGuardrailService;
    private final NotificationService notificationService;
    private final TransactionTemplate transactionTemplate;

    public PostService(
            PostRepository postRepository,
            CommentRepository commentRepository,
            UserRepository userRepository,
            BotRepository botRepository,
            RedisGuardrailService redisGuardrailService,
            NotificationService notificationService,
            PlatformTransactionManager transactionManager
    ) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.botRepository = botRepository;
        this.redisGuardrailService = redisGuardrailService;
        this.notificationService = notificationService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Transactional
    public PostResponse createPost(CreatePostRequest request) {
        validateAuthorExists(request.getAuthorId(), request.getIsAuthorBot());

        Post savedPost = postRepository.save(new Post(
                request.getAuthorId(),
                request.getIsAuthorBot(),
                request.getContent()
        ));

        return new PostResponse(
                savedPost.getId(),
                savedPost.getAuthorId(),
                savedPost.getIsAuthorBot(),
                savedPost.getContent(),
                savedPost.getCreatedAt(),
                0L
        );
    }

    @Transactional(readOnly = true)
    public PostResponse getPost(Long postId) {
        Post post = getPostOrThrow(postId);
        long viralityScore = redisGuardrailService.getViralityScore(postId);

        return new PostResponse(
                post.getId(),
                post.getAuthorId(),
                post.getIsAuthorBot(),
                post.getContent(),
                post.getCreatedAt(),
                viralityScore
        );
    }

    public CommentResponse createComment(Long postId, CreateCommentRequest request) {
        Post post = getPostOrThrow(postId);
        validateAuthorExists(request.getAuthorId(), request.getIsAuthorBot());

        Comment parentComment = resolveParentComment(postId, request.getParentId());
        int depthLevel = parentComment == null ? 1 : parentComment.getDepthLevel() + 1;
        if (depthLevel > MAX_DEPTH_LEVEL) {
            throw new BadRequestException("Maximum comment depth of 20 exceeded.");
        }

        return Boolean.TRUE.equals(request.getIsAuthorBot())
                ? createBotComment(post, request, parentComment, depthLevel)
                : createHumanComment(post, request, depthLevel);
    }

    public LikePostResponse likePost(Long postId, LikePostRequest request) {
        Post post = getPostOrThrow(postId);
        validateAuthorExists(request.getUserId(), false);

        long viralityScore = redisGuardrailService.reserveHumanLike(postId);
        safelyNotifyPostAuthor(post, request.getUserId(), false);

        return new LikePostResponse(postId, request.getUserId(), viralityScore, "Like accepted");
    }

    private CommentResponse createBotComment(Post post, CreateCommentRequest request, Comment parentComment, int depthLevel) {
        Long targetHumanUserId = resolveTargetHumanUserId(post, parentComment);
        RedisGuardrailService.BotCommentReservation reservation = redisGuardrailService.reserveBotComment(
                post.getId(),
                request.getAuthorId(),
                targetHumanUserId
        );

        Comment comment = new Comment(
                post.getId(),
                request.getAuthorId(),
                true,
                request.getContent(),
                request.getParentId(),
                depthLevel
        );

        try {
            Comment savedComment = persistComment(comment);
            safelyNotifyPostAuthor(post, request.getAuthorId(), true);
            return mapComment(savedComment, reservation.viralityScore(), reservation.botCount());
        } catch (RuntimeException exception) {
            redisGuardrailService.rollbackBotComment(post.getId(), request.getAuthorId(), targetHumanUserId);
            throw exception;
        }
    }

    private CommentResponse createHumanComment(Post post, CreateCommentRequest request, int depthLevel) {
        long viralityScore = redisGuardrailService.reserveHumanComment(post.getId());
        Comment comment = new Comment(
                post.getId(),
                request.getAuthorId(),
                false,
                request.getContent(),
                request.getParentId(),
                depthLevel
        );

        try {
            Comment savedComment = persistComment(comment);
            safelyNotifyPostAuthor(post, request.getAuthorId(), false);
            return mapComment(savedComment, viralityScore, redisGuardrailService.getBotCount(post.getId()));
        } catch (RuntimeException exception) {
            redisGuardrailService.rollbackHumanComment(post.getId());
            throw exception;
        }
    }

    private Comment persistComment(Comment comment) {
        Comment savedComment = transactionTemplate.execute(status -> commentRepository.save(comment));
        if (savedComment == null) {
            throw new IllegalStateException("Comment could not be saved");
        }
        return savedComment;
    }

    private Post getPostOrThrow(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id " + postId));
    }

    private Comment resolveParentComment(Long postId, Long parentId) {
        if (parentId == null) {
            return null;
        }

        return commentRepository.findByIdAndPostId(parentId, postId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent comment not found with id " + parentId));
    }

    private Long resolveTargetHumanUserId(Post post, Comment parentComment) {
        if (parentComment != null && Boolean.FALSE.equals(parentComment.getIsAuthorBot())) {
            return parentComment.getAuthorId();
        }

        if (Boolean.FALSE.equals(post.getIsAuthorBot())) {
            return post.getAuthorId();
        }

        return null;
    }

    private void validateAuthorExists(Long authorId, Boolean authorBot) {
        if (Boolean.TRUE.equals(authorBot)) {
            if (!botRepository.existsById(authorId)) {
                throw new ResourceNotFoundException("Bot not found with id " + authorId);
            }
            return;
        }

        if (!userRepository.existsById(authorId)) {
            throw new ResourceNotFoundException("User not found with id " + authorId);
        }
    }

    private void safelyNotifyPostAuthor(Post post, Long actorId, boolean actorBot) {
        try {
            notificationService.handlePostInteractionNotification(post, actorId, actorBot);
        } catch (RuntimeException exception) {
            log.error("Notification dispatch failed for post {}", post.getId(), exception);
        }
    }

    private CommentResponse mapComment(Comment comment, long viralityScore, long botCount) {
        return new CommentResponse(
                comment.getId(),
                comment.getPostId(),
                comment.getAuthorId(),
                comment.getIsAuthorBot(),
                comment.getContent(),
                comment.getParentId(),
                comment.getDepthLevel(),
                comment.getCreatedAt(),
                viralityScore,
                botCount
        );
    }
}
