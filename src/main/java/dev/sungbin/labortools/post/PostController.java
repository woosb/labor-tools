package dev.sungbin.labortools.post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PostController {

    private static final int PAGE_SIZE = 10;

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/posts";
    }

    @GetMapping("/posts")
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<Post> posts = postService.findPublished(PageRequest.of(page, PAGE_SIZE));
        model.addAttribute("posts", posts);
        return "post/list";
    }

    @GetMapping("/posts/{slug}")
    public String detail(@PathVariable String slug, Model model) {
        model.addAttribute("post", postService.readPublished(slug));
        return "post/detail";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
