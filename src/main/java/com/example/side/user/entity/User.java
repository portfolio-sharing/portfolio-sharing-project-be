package com.example.side.user.entity;

import com.example.side.comment.entity.Comments;
import com.example.side.post.entity.Post;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = {"posts", "comments"})
@Table(name = "member")
@EqualsAndHashCode(of = {"username", "password", "role"})
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    /**
     * username -> 리소스 서버에서 발급 받은 정보로 사용자를 특정할 아이디
     * ex) google_2398472981di
     */
    @Column(unique = true, nullable = false)
    private String username;
    private String password;

    /**
     * OAuth2에서 제공받는 이름(실명) -> 추후에 변경
     */
    private String nickname;

    private String email;

    @OneToMany(mappedBy = "user")
    private List<Post> posts = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<Comments> comments = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Builder
    public User(String username, String password, String nickname, String email, UserRole role) {
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.email = email;
        this.role = role;
    }

    /**
     * OAuth2 로그인 시 유저 이메일이나 이름이 바뀜을 업데이트
     */
    public void updateUserInfo(String email, String nickname) {
        this.email = email;
        this.nickname = nickname;
    }

}
