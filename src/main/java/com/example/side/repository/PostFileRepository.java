package com.example.side.repository;

import com.example.side.model.entity.PostFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface PostFileRepository extends JpaRepository<PostFile, Long> {
    // UserPostId로 UserPostFile 조회
    List<PostFile> findByUserPostId(Long userPostId);
}