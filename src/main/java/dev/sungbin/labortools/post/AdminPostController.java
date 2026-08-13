package dev.sungbin.labortools.post;

import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/posts")
public class AdminPostController {

    private final PostService postService;

    public AdminPostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("posts", postService.findAllForAdmin(PageRequest.of(page, 20)));
        return "admin/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", new PostForm());
        return "admin/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") PostForm form,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes) {
        rejectDuplicateSlug(form, null, bindingResult);
        if (bindingResult.hasErrors()) {
            return "admin/form";
        }
        Post saved = postService.create(form);
        redirectAttributes.addFlashAttribute("message", "저장했습니다.");
        return "redirect:/admin/posts/" + saved.getId() + "/edit";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Post post = postService.getById(id);
        model.addAttribute("form", PostForm.from(post));
        model.addAttribute("post", post);
        return "admin/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") PostForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        rejectDuplicateSlug(form, id, bindingResult);
        if (bindingResult.hasErrors()) {
            model.addAttribute("post", postService.getById(id));
            return "admin/form";
        }
        postService.update(id, form);
        redirectAttributes.addFlashAttribute("message", "수정했습니다.");
        return "redirect:/admin/posts/" + id + "/edit";
    }

    @PostMapping("/{id}/publish")
    public String publish(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        postService.publish(id);
        redirectAttributes.addFlashAttribute("message", "발행했습니다.");
        return "redirect:/admin/posts";
    }

    @PostMapping("/{id}/unpublish")
    public String unpublish(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        postService.unpublish(id);
        redirectAttributes.addFlashAttribute("message", "발행을 취소했습니다.");
        return "redirect:/admin/posts";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        postService.delete(id);
        redirectAttributes.addFlashAttribute("message", "삭제했습니다.");
        return "redirect:/admin/posts";
    }

    private void rejectDuplicateSlug(PostForm form, Long excludeId, BindingResult bindingResult) {
        if (form.getSlug() != null && postService.isSlugTaken(form.getSlug(), excludeId)) {
            bindingResult.rejectValue("slug", "duplicate", "이미 사용 중인 슬러그입니다.");
        }
    }
}
