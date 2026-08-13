package dev.sungbin.labortools.post;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownRendererTest {

    private final MarkdownRenderer renderer = new MarkdownRenderer();

    @Test
    void 기본_마크다운을_HTML_로_변환한다() {
        String html = renderer.render("# 제목\n\n본문 **강조**.");

        assertThat(html)
                .contains("<h1>제목</h1>")
                .contains("<strong>강조</strong>");
    }

    @Test
    void GFM_테이블_확장이_적용된다() {
        String md = """
                | 항목 | 일수 |
                |------|------|
                | 연차 | 15   |
                """;

        String html = renderer.render(md);

        assertThat(html)
                .contains("<table>")
                .contains("<th>항목</th>")
                .contains("<td>15</td>");
    }

    @Test
    void 빈_문자열은_빈_결과를_낸다() {
        assertThat(renderer.render("")).isEmpty();
    }
}
