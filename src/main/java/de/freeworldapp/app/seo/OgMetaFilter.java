package de.freeworldapp.app.seo;

import de.freeworldapp.app.offer.OfferRepository;
import de.freeworldapp.app.request.RequestRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Server-side OG-tag injection for detail-page URLs (AP 3.6). Messenger and
 * social crawlers don't execute the SPA's JavaScript, so link previews only
 * work if title/description/image are already in the served HTML. The SPA
 * itself ignores the extra tags. No-op in dev, where classpath:/static/
 * index.html doesn't exist (Vite serves the frontend).
 */
@Component
@Order(10)
public class OgMetaFilter extends OncePerRequestFilter {

    private static final Pattern DETAIL =
            Pattern.compile("^/(offers|requests)/([0-9a-fA-F-]{36})(/[^/]*)?$");

    private final OfferRepository offerRepo;
    private final RequestRepository requestRepo;
    private final String baseUrl;
    private volatile String indexHtml; // lazy cache; null = not present (dev)
    private volatile boolean indexLookedUp = false;

    public OgMetaFilter(OfferRepository offerRepo, RequestRepository requestRepo,
                        @Value("${app.base-url}") String baseUrl) {
        this.offerRepo = offerRepo;
        this.requestRepo = requestRepo;
        this.baseUrl = baseUrl.replaceAll("/$", "");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        if (!"GET".equalsIgnoreCase(req.getMethod())) { chain.doFilter(req, res); return; }
        Matcher m = DETAIL.matcher(req.getRequestURI());
        if (!m.matches()) { chain.doFilter(req, res); return; }

        String html = indexHtml();
        if (html == null) { chain.doFilter(req, res); return; }

        UUID id;
        try { id = UUID.fromString(m.group(2)); }
        catch (IllegalArgumentException e) { chain.doFilter(req, res); return; }

        String title = null, description = null, image = null;
        if ("offers".equals(m.group(1))) {
            var offer = offerRepo.findById(id).orElse(null);
            if (offer != null) {
                title = offer.getTitle();
                description = offer.getDescription();
                image = offer.getImageUrl();
            }
        } else {
            var request = requestRepo.findById(id).orElse(null);
            if (request != null) {
                title = request.getTitle();
                description = request.getDescription();
                image = request.getImageUrl();
            }
        }
        if (title == null) { chain.doFilter(req, res); return; }

        if (description != null && description.length() > 160) {
            description = description.substring(0, 157) + "…";
        }
        String url = baseUrl + req.getRequestURI();
        String absoluteImage = image == null ? null
                : (image.startsWith("http") ? image : baseUrl + image);

        StringBuilder meta = new StringBuilder();
        meta.append("<meta property=\"og:title\" content=\"").append(esc(title)).append(" — FreeWorld\">\n");
        if (description != null)
            meta.append("<meta property=\"og:description\" content=\"").append(esc(description)).append("\">\n")
                .append("<meta name=\"description\" content=\"").append(esc(description)).append("\">\n");
        meta.append("<meta property=\"og:url\" content=\"").append(esc(url)).append("\">\n");
        meta.append("<meta property=\"og:type\" content=\"article\">\n");
        meta.append("<meta property=\"og:site_name\" content=\"FreeWorld\">\n");
        if (absoluteImage != null) {
            meta.append("<meta property=\"og:image\" content=\"").append(esc(absoluteImage)).append("\">\n");
            meta.append("<meta name=\"twitter:card\" content=\"summary_large_image\">\n");
        }

        String out = html.replaceFirst("(?s)<title>.*?</title>",
                        Matcher.quoteReplacement("<title>" + esc(title) + " — FreeWorld</title>"))
                .replace("</head>", meta + "</head>");

        res.setStatus(200);
        res.setContentType("text/html;charset=UTF-8");
        res.getWriter().write(out);
    }

    private String indexHtml() {
        if (!indexLookedUp) {
            synchronized (this) {
                if (!indexLookedUp) {
                    try {
                        ClassPathResource resource = new ClassPathResource("static/index.html");
                        if (resource.exists()) {
                            indexHtml = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                        }
                    } catch (IOException ignored) {}
                    indexLookedUp = true;
                }
            }
        }
        return indexHtml;
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
