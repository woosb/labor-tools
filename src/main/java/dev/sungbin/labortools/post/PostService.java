package dev.sungbin.labortools.post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final TagRepository tagRepository;
    private final MarkdownRenderer markdownRenderer;

    public PostService(PostRepository postRepository,
                       TagRepository tagRepository,
                       MarkdownRenderer markdownRenderer) {
        this.postRepository = postRepository;
        this.tagRepository = tagRepository;
        this.markdownRenderer = markdownRenderer;
    }

    // ---------- 공개 ----------

    public Page<Post> findPublished(Pageable pageable) {
        return postRepository.findByStatusOrderByPublishedAtDesc(PostStatus.PUBLISHED, pageable);
    }

    @Transactional
    public Post readPublished(String slug) {
        Post post = postRepository.findBySlugAndStatus(slug, PostStatus.PUBLISHED)
                .orElseThrow(() -> new PostNotFoundException(slug));
        postRepository.incrementViewCount(post.getId());
        return post;
    }

    // ---------- 관리 ----------

    public Page<Post> findAllForAdmin(Pageable pageable) {
        return postRepository.findAllByOrderByUpdatedAtDesc(pageable);
    }

    public Post getById(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new PostNotFoundException("id=" + id));
    }

    @Transactional
    public Post create(PostForm form) {
        String html = markdownRenderer.render(form.getContentMd());
        Post post = new Post(form.getSlug(), form.getTitle(), form.getSummary(),
                form.getContentMd(), html);
        post.replaceTags(resolveTags(form.getTags()));
        return postRepository.save(post);
    }

    @Transactional
    public Post update(Long id, PostForm form) {
        Post post = getById(id);
        String html = markdownRenderer.render(form.getContentMd());
        post.edit(form.getSlug(), form.getTitle(), form.getSummary(), form.getContentMd(), html);
        post.replaceTags(resolveTags(form.getTags()));
        return post;   // 더티 체킹
    }

    @Transactional
    public void publish(Long id) {
        getById(id).publish();
    }

    @Transactional
    public void unpublish(Long id) {
        getById(id).unpublish();
    }

    @Transactional
    public void delete(Long id) {
        postRepository.deleteById(id);
    }

    public boolean isSlugTaken(String slug, Long excludeId) {
        return excludeId == null
                ? postRepository.existsBySlug(slug)
                : postRepository.existsBySlugAndIdNot(slug, excludeId);
    }

    // ---------- 내부 ----------

    private Set<Tag> resolveTags(String commaSeparated) {
        if (commaSeparated == null || commaSeparated.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(commaSeparated.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .map(name -> tagRepository.findByName(name)
                        .orElseGet(() -> tagRepository.save(new Tag(name, toSlug(name)))))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** 한글 태그도 URL 에 쓸 수 있게 최소한의 정규화만 한다. */
    private String toSlug(String name) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "-");
        return normalized.length() > 50 ? normalized.substring(0, 50) : normalized;
    }
}
