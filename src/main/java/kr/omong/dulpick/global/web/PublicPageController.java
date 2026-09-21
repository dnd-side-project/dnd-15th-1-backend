package kr.omong.dulpick.global.web;

import kr.omong.dulpick.domain.analytics.domain.AnalyticsActionEvent;
import kr.omong.dulpick.domain.analytics.domain.AnalyticsEventType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.util.UUID;

@RestController
public class PublicPageController {

    private static final String HTML_UTF_8 = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8";
    private static final String AASA_RESOURCE = "universal-link/apple-app-site-association";
    private final CsrfTokenRepository csrfTokenRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public PublicPageController(
            @Qualifier("opsCsrfTokenRepository") CsrfTokenRepository csrfTokenRepository,
            ApplicationEventPublisher eventPublisher,
            Clock clock
    ) {
        this.csrfTokenRepository = csrfTokenRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    public PublicPageController(
            CsrfTokenRepository csrfTokenRepository
    ) {
        this(csrfTokenRepository, null, Clock.systemUTC());
    }

    @GetMapping(value = "/", produces = HTML_UTF_8)
    public Resource home() {
        return page("index.html");
    }

    @GetMapping(value = "/privacy", produces = HTML_UTF_8)
    public Resource privacy() {
        return page("privacy.html");
    }

    @GetMapping(value = "/privacy/history", produces = HTML_UTF_8)
    public Resource privacyHistory() {
        return page("privacy-history.html");
    }

    @GetMapping(value = "/privacy/history/v1.0", produces = HTML_UTF_8)
    public Resource privacyHistoryV1_0() {
        return page("privacy-v1.0.html");
    }

    @GetMapping(value = "/privacy/history/v1.1", produces = HTML_UTF_8)
    public Resource privacyHistoryV1_1() {
        return page("privacy-v1.1.html");
    }

    @GetMapping(value = "/terms", produces = HTML_UTF_8)
    public Resource terms() {
        return page("terms.html");
    }

    @GetMapping(value = "/terms/history", produces = HTML_UTF_8)
    public Resource termsHistory() {
        return page("terms-history.html");
    }

    @GetMapping(value = "/terms/history/v1.0", produces = HTML_UTF_8)
    public Resource termsHistoryV1_0() {
        return page("terms-v1.0.html");
    }

    @GetMapping(value = "/marketing", produces = HTML_UTF_8)
    public Resource marketing() {
        return page("marketing.html");
    }

    @GetMapping(value = "/marketing/history", produces = HTML_UTF_8)
    public Resource marketingHistory() {
        return page("marketing-history.html");
    }

    @GetMapping(value = "/marketing/history/v1.0", produces = HTML_UTF_8)
    public Resource marketingHistoryV1_0() {
        return page("marketing-v1.0.html");
    }

    @GetMapping(value = "/connect", produces = HTML_UTF_8)
    public Resource connect() {
        return page("connect.html");
    }

    @GetMapping(value = "/download", produces = HTML_UTF_8)
    public Resource download() {
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new AnalyticsActionEvent(
                    "DOWNLOAD_PAGE_VISITED:%s".formatted(UUID.randomUUID()),
                    AnalyticsEventType.DOWNLOAD_PAGE_VISITED,
                    null,
                    null,
                    "DOWNLOAD_PAGE",
                    null,
                    clock.instant()
            ));
        }
        return page("download.html");
    }

    @GetMapping(value = "/ops/login", produces = HTML_UTF_8)
    public Resource opsLogin(HttpServletRequest request, HttpServletResponse response) {
        ensureCsrfToken(request, response);
        return page("ops-login.html");
    }

    @GetMapping(value = "/ops", produces = HTML_UTF_8)
    public Resource opsDashboard(HttpServletRequest request, HttpServletResponse response) {
        ensureCsrfToken(request, response);
        return page("ops-dashboard.html");
    }

    @GetMapping(value = "/ops/places", produces = HTML_UTF_8)
    public Resource opsPlaces(HttpServletRequest request, HttpServletResponse response) {
        ensureCsrfToken(request, response);
        return page("ops-places.html");
    }

    @GetMapping(value = "/ops/notices", produces = HTML_UTF_8)
    public Resource opsNotices(HttpServletRequest request, HttpServletResponse response) {
        ensureCsrfToken(request, response);
        return page("ops-notices.html");
    }

    @GetMapping(value = "/favicon.ico", produces = "image/png")
    public Resource favicon() {
        return new ClassPathResource("static/favicon.png");
    }

    @GetMapping(
            value = "/.well-known/apple-app-site-association",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Resource appleAppSiteAssociation() {
        return new ClassPathResource(AASA_RESOURCE);
    }

    private Resource page(String fileName) {
        return new ClassPathResource("static/" + fileName);
    }

    private void ensureCsrfToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        csrfTokenRepository.saveToken(
                csrfTokenRepository.loadDeferredToken(request, response).get(),
                request,
                response
        );
    }
}
