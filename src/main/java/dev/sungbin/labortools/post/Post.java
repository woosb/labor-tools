package dev.sungbin.labortools.post;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "post")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 200)
    private String slug;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(length = 500)
    private String summary;

    @Column(name = "content_md", nullable = false, columnDefinition = "text")
    private String contentMd;

    @Column(name = "content_html", nullable = false, columnDefinition = "text")
    private String contentHtml;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostStatus status = PostStatus.DRAFT;

    @Column(name = "view_count", nullable = false)
    private long viewCount;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "post_tag",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new LinkedHashSet<>();

    protected Post() {
    }

    public Post(String slug, String title, String summary, String contentMd, String contentHtml) {
        this.slug = slug;
        this.title = title;
        this.summary = summary;
        this.contentMd = contentMd;
        this.contentHtml = contentHtml;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void edit(String slug, String title, String summary, String contentMd, String contentHtml) {
        this.slug = slug;
        this.title = title;
        this.summary = summary;
        this.contentMd = contentMd;
        this.contentHtml = contentHtml;
    }

    /** 최초 발행 시에만 publishedAt 을 세팅한다. 재발행해도 날짜가 밀리지 않게. */
    public void publish() {
        this.status = PostStatus.PUBLISHED;
        if (this.publishedAt == null) {
            this.publishedAt = Instant.now();
        }
    }

    public void unpublish() {
        this.status = PostStatus.DRAFT;
    }

    public void replaceTags(Set<Tag> newTags) {
        this.tags.clear();
        this.tags.addAll(newTags);
    }

    public boolean isPublished() {
        return this.status == PostStatus.PUBLISHED;
    }

    public Long getId() { return id; }
    public String getSlug() { return slug; }
    public String getTitle() { return title; }
    public String getSummary() { return summary; }
    public String getContentMd() { return contentMd; }
    public String getContentHtml() { return contentHtml; }
    public PostStatus getStatus() { return status; }
    public long getViewCount() { return viewCount; }
    public Instant getPublishedAt() { return publishedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Set<Tag> getTags() { return tags; }
}
