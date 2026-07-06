package de.freeworldapp.app.seo;

import de.freeworldapp.app.offer.Offer;
import de.freeworldapp.app.offer.OfferRepository;
import de.freeworldapp.app.request.Request;
import de.freeworldapp.app.request.RequestRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.List;

/** sitemap.xml + robots.txt (AP 3.6). */
@RestController
public class SeoController {

    private final OfferRepository offerRepo;
    private final RequestRepository requestRepo;
    private final String baseUrl;

    public SeoController(OfferRepository offerRepo, RequestRepository requestRepo,
                         @Value("${app.base-url}") String baseUrl) {
        this.offerRepo = offerRepo;
        this.requestRepo = requestRepo;
        this.baseUrl = baseUrl.replaceAll("/$", "");
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String sitemap() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        for (String path : List.of("/", "/offers", "/requests", "/impressum", "/datenschutz", "/terms")) {
            xml.append("  <url><loc>").append(baseUrl).append(path).append("</loc></url>\n");
        }
        offerRepo.findAll().stream()
                .filter(o -> o.getStatus() != Offer.Status.GIVEN && !o.getOfferedBy().isBlocked())
                .forEach(o -> xml.append("  <url><loc>").append(baseUrl).append("/offers/")
                        .append(o.getId()).append("/").append(Slugs.of(o.getTitle()))
                        .append("</loc><lastmod>")
                        .append(DateTimeFormatter.ISO_INSTANT.format(o.getCreatedAt()))
                        .append("</lastmod></url>\n"));
        requestRepo.findAll().stream()
                .filter(r -> r.getStatus() != Request.Status.FULFILLED && !r.getRequestedBy().isBlocked())
                .forEach(r -> xml.append("  <url><loc>").append(baseUrl).append("/requests/")
                        .append(r.getId()).append("/").append(Slugs.of(r.getTitle()))
                        .append("</loc><lastmod>")
                        .append(DateTimeFormatter.ISO_INSTANT.format(r.getCreatedAt()))
                        .append("</lastmod></url>\n"));
        xml.append("</urlset>\n");
        return xml.toString();
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public String robots() {
        return """
                User-agent: *
                Disallow: /api/
                Disallow: /admin
                Disallow: /settings
                Disallow: /messages
                Allow: /

                Sitemap: %s/sitemap.xml
                """.formatted(baseUrl);
    }
}
