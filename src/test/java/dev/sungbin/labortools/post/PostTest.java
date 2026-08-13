package dev.sungbin.labortools.post;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PostTest {

    private Post newPost() {
        return new Post("annual-leave", "연차 계산", "요약", "# 본문", "<h1>본문</h1>");
    }

    @Test
    void 새_글은_DRAFT_상태다() {
        Post post = newPost();

        assertThat(post.getStatus()).isEqualTo(PostStatus.DRAFT);
        assertThat(post.isPublished()).isFalse();
        assertThat(post.getPublishedAt()).isNull();
        assertThat(post.getViewCount()).isZero();
    }

    @Test
    void 발행하면_상태와_발행일시가_세팅된다() {
        Post post = newPost();

        post.publish();

        assertThat(post.isPublished()).isTrue();
        assertThat(post.getStatus()).isEqualTo(PostStatus.PUBLISHED);
        assertThat(post.getPublishedAt()).isNotNull();
    }

    @Test
    void 재발행해도_최초_발행일시는_유지된다() {
        Post post = newPost();
        post.publish();
        Instant first = post.getPublishedAt();

        post.unpublish();
        post.publish();

        assertThat(post.getPublishedAt()).isEqualTo(first);
    }

    @Test
    void 발행_취소는_DRAFT_로_되돌리되_발행일시는_남긴다() {
        Post post = newPost();
        post.publish();

        post.unpublish();

        assertThat(post.getStatus()).isEqualTo(PostStatus.DRAFT);
        assertThat(post.getPublishedAt()).isNotNull();
    }

    @Test
    void 수정하면_본문과_메타가_바뀐다() {
        Post post = newPost();

        post.edit("new-slug", "새 제목", "새 요약", "## 새 본문", "<h2>새 본문</h2>");

        assertThat(post.getSlug()).isEqualTo("new-slug");
        assertThat(post.getTitle()).isEqualTo("새 제목");
        assertThat(post.getSummary()).isEqualTo("새 요약");
        assertThat(post.getContentMd()).isEqualTo("## 새 본문");
        assertThat(post.getContentHtml()).isEqualTo("<h2>새 본문</h2>");
    }

    @Test
    void 태그_교체는_기존_태그를_모두_치환한다() {
        Post post = newPost();
        post.replaceTags(Set.of(new Tag("연차", "연차")));

        post.replaceTags(Set.of(new Tag("근로기준법", "근로기준법")));

        assertThat(post.getTags())
                .extracting(Tag::getName)
                .containsExactly("근로기준법");
    }

    @Test
    void 태그를_빈_집합으로_교체하면_모두_사라진다() {
        Post post = newPost();
        post.replaceTags(Set.of(new Tag("연차", "연차")));

        post.replaceTags(Set.of());

        assertThat(post.getTags()).isEmpty();
    }
}
