package kr.omong.dulpick.global.web;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PublicPageController {

    private static final String HTML_UTF_8 = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8";

    @GetMapping(value = "/", produces = HTML_UTF_8)
    public Resource home() {
        return page("index.html");
    }

    @GetMapping(value = "/privacy", produces = HTML_UTF_8)
    public Resource privacy() {
        return page("privacy.html");
    }

    @GetMapping(value = "/terms", produces = HTML_UTF_8)
    public Resource terms() {
        return page("terms.html");
    }

    private Resource page(String fileName) {
        return new ClassPathResource("static/" + fileName);
    }
}
