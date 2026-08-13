package dev.sungbin.labortools.post;

import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 마크다운을 HTML 로 변환한다.
 * 작성자가 본인 한 명뿐이므로 별도 sanitize 는 하지 않는다.
 * 외부 입력을 받게 되는 순간(댓글 등) 반드시 OWASP Java HTML Sanitizer 를 끼워야 한다.
 */
@Component
public class MarkdownRenderer {

    private final Parser parser;
    private final HtmlRenderer renderer;

    public MarkdownRenderer() {
        var extensions = List.of(TablesExtension.create());
        this.parser = Parser.builder().extensions(extensions).build();
        this.renderer = HtmlRenderer.builder().extensions(extensions).build();
    }

    public String render(String markdown) {
        Node document = parser.parse(markdown);
        return renderer.render(document);
    }
}
