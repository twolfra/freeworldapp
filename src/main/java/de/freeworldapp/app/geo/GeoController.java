package de.freeworldapp.app.geo;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Public lookup of the local PLZ table — no external geocoding calls. */
@RestController
@RequestMapping("/api/geo")
public class GeoController {

    private final PlzGeoRepository repo;

    public GeoController(PlzGeoRepository repo) {
        this.repo = repo;
    }

    /** Autocomplete by PLZ prefix or city-name prefix; max 10 results. */
    @GetMapping("/postal")
    public ResponseEntity<?> postal(@RequestParam String q) {
        if (q == null || q.strip().length() < 2) return ResponseEntity.ok(List.of());
        List<Map<String, Object>> out = repo.autocomplete(q.strip(), PageRequest.of(0, 10)).stream()
                .map(p -> Map.<String, Object>of(
                        "plz", p.getPlz(), "city", p.getCity(),
                        "lat", p.getLat(), "lon", p.getLon()))
                .toList();
        return ResponseEntity.ok(out);
    }
}
