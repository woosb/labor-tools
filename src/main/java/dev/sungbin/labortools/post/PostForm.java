package dev.sungbin.labortools.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 관리자 글 작성/수정 폼.
 * tags 는 "연차,근로기준법" 형태의 콤마 구분 문자열로 받는다.
 */
public class PostForm {

    private Long id;

    @NotBlank(message = "URL 슬러그를 입력하세요.")
    @Size(max = 200)
    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
             message = "슬러그는 영소문자, 숫자, 하이픈만 사용할 수 있습니다.")
    private String slug;

    @NotBlank(message = "제목을 입력하세요.")
    @Size(max = 300)
    private String title;

    @Size(max = 500)
    private String summary;

    @NotBlank(message = "본문을 입력하세요.")
    private String contentMd;

    private String tags = "";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getContentMd() { return contentMd; }
    public void setContentMd(String contentMd) { this.contentMd = contentMd; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public static PostForm from(Post post) {
        PostForm form = new PostForm();
        form.id = post.getId();
        form.slug = post.getSlug();
        form.title = post.getTitle();
        form.summary = post.getSummary();
        form.contentMd = post.getContentMd();
        form.tags = String.join(",", post.getTags().stream().map(Tag::getName).toList());
        return form;
    }
}
