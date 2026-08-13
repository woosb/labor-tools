package dev.sungbin.labortools.post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findByStatusOrderByPublishedAtDesc(PostStatus status, Pageable pageable);

    Optional<Post> findBySlug(String slug);

    Optional<Post> findBySlugAndStatus(String slug, PostStatus status);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    Page<Post> findAllByOrderByUpdatedAtDesc(Pageable pageable);

    /**
     * 조회수는 엔티티 더티체킹으로 올리지 않는다.
     * 동시 조회 시 갱신 유실이 생기고, updatedAt 도 같이 밀려버리기 때문.
     */
    @Modifying
    @Query("update Post p set p.viewCount = p.viewCount + 1 where p.id = :id")
    void incrementViewCount(@Param("id") Long id);
}
