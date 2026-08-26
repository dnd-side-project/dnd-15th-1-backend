package kr.omong.dulpick.domain.notification.application.admin;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class EmailTemplateRenderer {

    public String renderPolicyNotice(String nickname, String title, String body) {
        try {
            String template = new ClassPathResource("templates/email/announcement.html")
                    .getContentAsString(StandardCharsets.UTF_8);
            return template
                    .replace("{{nickname}}", escape(nickname))
                    .replace("{{title}}", escape(title))
                    .replace("{{body}}", escapeMultiline(body))
                    .replace("{{year}}", String.valueOf(java.time.Year.now().getValue()))
                    .replace("{{unsubscribeUrl}}", "#");
        } catch (IOException exception) {
            throw new IllegalStateException("Email template loading failed", exception);
        }
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private String escapeMultiline(String value) {
        return escape(value).replace("\n", "<br>");
    }
}
