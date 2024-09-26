package com.example.side.post.service;

import com.example.side.Exception.CustomException;
import com.example.side.auth.CustomUserDetails;
import com.example.side.comments.dto.response.CommentsResponse;
import com.example.side.post.entity.PortfolioPost;
import com.example.side.post.entity.PostType;
import com.example.side.post.like.repository.PostLikeRepository;
import com.example.side.post.repository.PortfolioPostRepository;
import com.example.side.post.tag.entity.PostTag;
import com.example.side.post.tag.entity.PostTagRepository;
import com.example.side.post.tag.entity.Tag;
import com.example.side.post.tag.entity.TagRepository;
import com.example.side.user.entity.User;
import com.example.side.user.repository.UserRepository;
import com.example.side.post.dto.request.PortfolioPostRequest;
import com.example.side.post.dto.response.PortfolioPostResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static com.example.side.Exception.ErrorCode.NOT_FOUND_POST;

@RequiredArgsConstructor
@Service
public class PortfolioPostService {

    private static final String NO_PERMISSION = "이 페이지에 대한 권한이 없습니다.";

    private final PortfolioPostRepository portfolioPostRepository;
    private final PostLikeRepository postLikeRepository;
    private final TagRepository tagRepository;
    private final PostTagRepository postTagRepository;

    // 생성
    @Transactional
    public PortfolioPostResponse createPost(PortfolioPostRequest portfolioPostRequest, CustomUserDetails userDetails) {

        PortfolioPost portfolioPost = PortfolioPost.builder()
                .url(portfolioPostRequest.getUrl())
                .technologyStack(portfolioPostRequest.getTechnologyStack())
                .img(portfolioPostRequest.getImg())

                .build();
        portfolioPost.setPostType(PostType.PORTFOLIO);
        portfolioPost.setTitle(portfolioPostRequest.getTitle());
        portfolioPost.setDescription(portfolioPostRequest.getDescription());
        portfolioPost.setUser(userDetails.getUser());
        portfolioPost.setCreatedAt(LocalDateTime.now());
        portfolioPost.setUpdatedAt(LocalDateTime.now());
        portfolioPost.setLikeCount(0L);
        portfolioPost.setViewCount(0L);
        portfolioPostRepository.save(portfolioPost);

        processTags(portfolioPost, portfolioPostRequest.getTags());
//        processFiles(portfolioPost, portfolioPostRequest.getFiles());

        return PortfolioPostResponse.from(portfolioPost);

    }

    private void processTags(PortfolioPost post, List<String> tagNames) {
        if (tagNames != null && !tagNames.isEmpty()) {
            for (String name : tagNames) {
                Tag tag = tagRepository.findByName(name)
                        .orElseGet(() -> {
                            Tag newTag = new Tag();
                            newTag.setName(name);
                            return tagRepository.save(newTag);
                        });
                PostTag postTag = new PostTag();
                postTag.setPost(post);
                postTag.setTag(tag);
                postTagRepository.save(postTag);
            }
        }
    }

//    private void processFiles(PortfolioPost post, List<MultipartFile> files) {
//        if (files != null && !files.isEmpty()) {
//            for (MultipartFile file : files) {
//                PostFile postFile = new PostFile();
//                postFile.setUserPostId(post.getId());
//                postFile.setFileName(file.getOriginalFilename());
//                postFile.setFileSize(file.getSize());
//                postFile.setFileType(file.getContentType());
//                postFile.setFileOriginname(file.getOriginalFilename());
//                postFileRepository.save(postFile);
//
//                saveFile(file);
//            }
//        }

    // 수정
    @Transactional
    public PortfolioPostResponse updatePost(Long portfolioPostId, PortfolioPostRequest portfolioPostRequest, CustomUserDetails userDetails) {

        PortfolioPost portfolioPost = portfolioPostRepository.findById(portfolioPostId)
                .orElseThrow(() -> new RuntimeException("게시글이 존재하지 않습니다."));

        portfolioPost.setTitle(portfolioPostRequest.getTitle());
        portfolioPost.setDescription(portfolioPostRequest.getDescription());
        portfolioPost.setUrl(portfolioPostRequest.getUrl());
        portfolioPost.setTechnologyStack(portfolioPostRequest.getTechnologyStack());
//        portfolioPost.setImg(portfolioPostRequest.getImg());

        portfolioPost = portfolioPostRepository.save(portfolioPost);

        // 태그 삭제
//        postTagRepository.deleteByPost(portfolioPost);
//
        return PortfolioPostResponse.from(portfolioPost);
    }


    // 삭제
    @Transactional
    public HashMap<String, Long> deletePost(Long postId, CustomUserDetails userDetails) {
        PortfolioPost portfolioPost = getPostById(postId);

        validateUserPermission(portfolioPost, userDetails);

        portfolioPostRepository.deleteById(postId);
        HashMap<String, Long> responseId = new HashMap<>();
        responseId.put("postId", portfolioPost.getId());
        return responseId;
    }

    // 전체 조회
    @Transactional
    public List<PortfolioPostResponse> portfolioPosts() {
        return portfolioPostRepository.findAll().stream()
                .map(PortfolioPostResponse::new)
                .collect(Collectors.toList());
    }

    // 상세 조회
    @Transactional
    public PortfolioPostResponse getPostId(Long postId, User user) {
        PortfolioPost portfolioPost = getPostById(postId);
        List<CommentsResponse> commentsResponses = getSortedComments(portfolioPost);
        boolean isLiked = isPostLikedByUser(postId, user);
        return PortfolioPostResponse.of(portfolioPost, commentsResponses, isLiked);
    }

//    // 필터링 조회
//    @Transactional
//    public List<PortfolioPost> getFilteredPosts(List<String> tags, LocalDateTime startDate, LocalDateTime endDate, String sortBy) {
//        return portfolioPostRepository.findByPosts(tags, startDate, endDate, sortBy);
//    }

    // 검색
//    @Transactional
//    public List<PortfolioPost> searchPosts(String keyword) {
//        return portfolioPostRepository.findByTitle(keyword);
//    }

    // 유틸 메소드: 게시물 ID로 찾기
    private PortfolioPost getPostById(Long postId) {
        return portfolioPostRepository.findById(postId)
                .orElseThrow(() -> new CustomException(NOT_FOUND_POST));
    }

    // 유틸 메소드: 유저 권한 검증
    private void validateUserPermission(PortfolioPost portfolioPost, CustomUserDetails userDetails) {
        if (!portfolioPost.getUser().getId().equals(userDetails.getUser().getId())) {
            throw new IllegalArgumentException(NO_PERMISSION);
        }
    }

    // 유틸 메소드: 댓글 정렬 및 응답 생성
    private List<CommentsResponse> getSortedComments(PortfolioPost portfolioPost) {
        return portfolioPost.getComments().stream()
                .sorted((c1, c2) -> c2.getCreatedAt().compareTo(c1.getCreatedAt()))
                .map(CommentsResponse::new)
                .collect(Collectors.toList());
    }

    // 유틸 메소드: 게시물 좋아요 여부 확인
    private boolean isPostLikedByUser(Long postId, User user) {
        return postLikeRepository.findByPostIdAndUserId(postId, user.getId()) != null;
    }
}
