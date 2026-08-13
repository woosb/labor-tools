package dev.sungbin.labortools.post;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private TagRepository tagRepository;

    private PostService postService;

    @BeforeEach
    void setUp() {
        postService = new PostService(postRepository, tagRepository, new MarkdownRenderer());
    }

    private PostForm form(String tags) {
        PostForm form = new PostForm();
        form.setSlug("annual-leave");
        form.setTitle("연차 계산");
        form.setSummary("요약");
        form.setContentMd("# 본문");
        form.setTags(tags);
        return form;
    }

    private void returnSavedArgument() {
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---------- 생성 ----------

    @Test
    void 생성하면_마크다운이_HTML_로_렌더링되어_저장된다() {
        returnSavedArgument();

        Post saved = postService.create(form(""));

        assertThat(saved.getSlug()).isEqualTo("annual-leave");
        assertThat(saved.getContentMd()).isEqualTo("# 본문");
        assertThat(saved.getContentHtml()).contains("<h1>본문</h1>");
        assertThat(saved.getStatus()).isEqualTo(PostStatus.DRAFT);
    }

    @Test
    void 태그가_비어_있으면_태그_저장소를_건드리지_않는다() {
        returnSavedArgument();

        Post saved = postService.create(form("   "));

        assertThat(saved.getTags()).isEmpty();
        verifyNoInteractions(tagRepository);
    }

    @Test
    void 태그는_공백을_제거하고_중복을_제거한_뒤_입력_순서를_지킨다() {
        returnSavedArgument();
        when(tagRepository.findByName(any())).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenAnswer(inv -> inv.getArgument(0));

        Post saved = postService.create(form(" 연차 , 근로기준법 ,연차, "));

        assertThat(saved.getTags())
                .extracting(Tag::getName)
                .containsExactly("연차", "근로기준법");
    }

    @Test
    void 이미_존재하는_태그는_새로_만들지_않고_재사용한다() {
        returnSavedArgument();
        Tag existing = new Tag("연차", "연차");
        when(tagRepository.findByName("연차")).thenReturn(Optional.of(existing));

        Post saved = postService.create(form("연차"));

        assertThat(saved.getTags()).containsExactly(existing);
        verify(tagRepository, never()).save(any(Tag.class));
    }

    @Test
    void 새_태그의_슬러그는_소문자와_하이픈으로_정규화된다() {
        returnSavedArgument();
        when(tagRepository.findByName(any())).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenAnswer(inv -> inv.getArgument(0));

        postService.create(form("Annual Leave"));

        ArgumentCaptor<Tag> captor = ArgumentCaptor.forClass(Tag.class);
        verify(tagRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Annual Leave");
        assertThat(captor.getValue().getSlug()).isEqualTo("annual-leave");
    }

    @Test
    void 태그_슬러그는_50자를_넘지_않는다() {
        returnSavedArgument();
        when(tagRepository.findByName(any())).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenAnswer(inv -> inv.getArgument(0));

        postService.create(form("a".repeat(60)));

        ArgumentCaptor<Tag> captor = ArgumentCaptor.forClass(Tag.class);
        verify(tagRepository).save(captor.capture());
        assertThat(captor.getValue().getSlug()).hasSize(50);
    }

    // ---------- 수정 ----------

    @Test
    void 수정하면_본문이_다시_렌더링된다() {
        Post post = new Post("old", "옛 제목", "옛 요약", "# 옛 본문", "<h1>옛 본문</h1>");
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        PostForm form = form("");
        form.setContentMd("## 새 본문");
        Post updated = postService.update(1L, form);

        assertThat(updated.getTitle()).isEqualTo("연차 계산");
        assertThat(updated.getContentHtml()).contains("<h2>새 본문</h2>");
        verify(postRepository, never()).save(any(Post.class));   // 더티 체킹에 맡긴다
    }

    @Test
    void 없는_글을_수정하면_예외가_난다() {
        when(postRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.update(99L, form("")))
                .isInstanceOf(PostNotFoundException.class);
    }

    // ---------- 조회 ----------

    @Test
    void 공개_글을_읽으면_조회수가_증가한다() {
        Post post = new Post("annual-leave", "연차 계산", "요약", "# 본문", "<h1>본문</h1>");
        ReflectionTestUtils.setField(post, "id", 42L);
        when(postRepository.findBySlugAndStatus("annual-leave", PostStatus.PUBLISHED))
                .thenReturn(Optional.of(post));

        Post found = postService.readPublished("annual-leave");

        assertThat(found).isSameAs(post);
        verify(postRepository).incrementViewCount(42L);
    }

    @Test
    void 발행되지_않은_글은_공개_조회에서_찾을_수_없다() {
        when(postRepository.findBySlugAndStatus("draft-only", PostStatus.PUBLISHED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.readPublished("draft-only"))
                .isInstanceOf(PostNotFoundException.class)
                .hasMessageContaining("draft-only");

        verify(postRepository, never()).incrementViewCount(any());
    }

    // ---------- 슬러그 중복 ----------

    @Test
    void 신규_작성_시에는_전체에서_슬러그_중복을_본다() {
        when(postRepository.existsBySlug("annual-leave")).thenReturn(true);

        assertThat(postService.isSlugTaken("annual-leave", null)).isTrue();
        verify(postRepository, never()).existsBySlugAndIdNot(any(), any());
    }

    @Test
    void 수정_시에는_자기_자신을_중복에서_제외한다() {
        when(postRepository.existsBySlugAndIdNot("annual-leave", 1L)).thenReturn(false);

        assertThat(postService.isSlugTaken("annual-leave", 1L)).isFalse();
        verify(postRepository, never()).existsBySlug(eq("annual-leave"));
    }
}
