package dev.sungbin.labortools.post;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PostFormTest {

    @Test
    void 엔티티에서_폼으로_변환한다() {
        Post post = new Post("annual-leave", "연차 계산", "요약", "# 본문", "<h1>본문</h1>");
        ReflectionTestUtils.setField(post, "id", 7L);

        PostForm form = PostForm.from(post);

        assertThat(form.getId()).isEqualTo(7L);
        assertThat(form.getSlug()).isEqualTo("annual-leave");
        assertThat(form.getTitle()).isEqualTo("연차 계산");
        assertThat(form.getSummary()).isEqualTo("요약");
        assertThat(form.getContentMd()).isEqualTo("# 본문");
    }

    @Test
    void 태그는_콤마로_이어붙여_폼에_담는다() {
        Post post = new Post("annual-leave", "연차 계산", "요약", "# 본문", "<h1>본문</h1>");
        Set<Tag> tags = new LinkedHashSet<>();
        tags.add(new Tag("연차", "연차"));
        tags.add(new Tag("근로기준법", "근로기준법"));
        post.replaceTags(tags);

        assertThat(PostForm.from(post).getTags()).isEqualTo("연차,근로기준법");
    }

    @Test
    void 태그가_없으면_빈_문자열이다() {
        Post post = new Post("annual-leave", "연차 계산", "요약", "# 본문", "<h1>본문</h1>");

        assertThat(PostForm.from(post).getTags()).isEmpty();
    }
}
