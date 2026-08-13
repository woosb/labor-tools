package dev.sungbin.labortools.post;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class PostNotFoundException extends RuntimeException {

    public PostNotFoundException(String slug) {
        super("글을 찾을 수 없습니다: " + slug);
    }
}
