package com.example.side.post.service;

import com.example.side.Exception.CustomException;
import com.example.side.comments.dto.response.CommentsResponse;
import com.example.side.comments.entity.Comments;
import com.example.side.config.UserDetailsImpl;
import com.example.side.post.entity.CommunityPost;
import com.example.side.post.like.entity.PostLike;
import com.example.side.post.like.repository.PostLikeRepository;
import com.example.side.post.repository.CommunityPostRepository;
import com.example.side.user.entity.User;
import com.example.side.user.repository.UserRepository;
import com.example.side.post.dto.request.CommunityPostRequest;
import com.example.side.post.dto.response.CommunityPostResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static com.example.side.Exception.ErrorCode.NOT_FOUND_POST;

@RequiredArgsConstructor
@Service
public class CommunityPostService {
    private final UserRepository userRepository;
    private final CommunityPostRepository communityPostRepository;
    private final PostLikeRepository postLikeRepository;

    @Transactional //생성
    public CommunityPostResponse createPost(CommunityPostRequest communityPostRequest, UserDetailsImpl userDetails) {
        CommunityPost communityPost = new CommunityPost(communityPostRequest, userDetails.getUser());
        CommunityPost savedCommunityPost = communityPostRepository.save(communityPost);
        return new CommunityPostResponse(savedCommunityPost);
    }

    @Transactional //수정
    public CommunityPostResponse updatePost(Long postId, CommunityPostRequest postRequest, UserDetailsImpl userDetails) {
        CommunityPost communityPost = communityPostRepository.findById(postId).orElseThrow(()
                -> new IllegalArgumentException("Unknown post ID: " + postId));
        if (!communityPost.getUser().getId().equals(userDetails.getUser().getId())) {
            throw new IllegalArgumentException("이 페이지에 대한 권한이 없습니다.");
        }
        communityPost.update(postRequest);
        CommunityPost updatedCommunityPost = communityPostRepository.save(communityPost);
        return new CommunityPostResponse(updatedCommunityPost);
    }

    @Transactional //삭제
    public HashMap<String, Long> deletePost(Long postId, UserDetailsImpl userDetails) {
        CommunityPost post = communityPostRepository.findById(postId).orElseThrow(()
                -> new IllegalArgumentException("Unknown post ID: " + postId));
        if (!post.getUser().getId().equals(userDetails.getUser().getId())) {
            throw new IllegalArgumentException("You can only delete your own posts");
        }
        communityPostRepository.deleteById(postId);
        HashMap<String, Long> responseId = new HashMap<>();
        responseId.put("postId", post.getId());
        return responseId;
    }
    //조회
    public List<CommunityPostResponse> communityPosts() {
        return communityPostRepository.findAll().stream()
                .map(CommunityPostResponse::new)
                .collect(Collectors.toList());
    }
    //상세조회
    public CommunityPostResponse getPostId(Long postId, User user){
        CommunityPost communityPost = communityPostRepository.findById(postId).orElseThrow(()->new CustomException(NOT_FOUND_POST));
        communityPost.getComments().sort(Comparator.comparing(Comments::getCreatedAt).reversed());
        List<CommentsResponse> commentsResponses = new ArrayList<>();
        for(Comments comments : communityPost.getComments()){
            commentsResponses.add(new CommentsResponse(comments));
        }
        PostLike postLike = postLikeRepository.findByPostIdAndUserId(postId,user.getId());
        if(postLike != null){
            return CommunityPostResponse.of(communityPost,commentsResponses,true);
        }
        else{
            return CommunityPostResponse.of(communityPost,commentsResponses,false);
        }
    }
    //검색
    @Transactional(readOnly = true)
    public List<CommunityPostResponse> searchPostsByTitle(String title) {
        List<CommunityPost> posts = communityPostRepository.findPostsByTitle(title);
        return posts.stream()
                .map(CommunityPostResponse::new)
                .collect(Collectors.toList());
    }
    //카테고리 검색
    @Transactional(readOnly = true)
    public List<CommunityPostResponse> findByCategory(String category) {
        List<CommunityPost> posts = communityPostRepository.findByCategory(category);
        return posts.stream()
                .map(CommunityPostResponse::new)
                .collect(Collectors.toList());
    }
}
