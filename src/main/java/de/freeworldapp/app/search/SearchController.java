package de.freeworldapp.app.search;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One combinable search endpoint (AP 3.1 + 3.5) instead of a growing
 * query-param zoo on the list endpoints. Uses native SQL because Postgres
 * full-text (websearch_to_tsquery) and Haversine don't map to Criteria/
 * Specifications cleanly; every value is bound, never concatenated.
 */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private static final String HAVERSINE =
            "6371.0 * acos(LEAST(1.0, cos(radians(:lat)) * cos(radians(p.lat)) " +
            "* cos(radians(p.lon) - radians(:lon)) + sin(radians(:lat)) * sin(radians(p.lat))))";

    private final NamedParameterJdbcTemplate jdbc;

    public SearchController(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public ResponseEntity<?> search(
            @RequestParam(defaultValue = "offers") String type,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon,
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "false") boolean withImage,
            @RequestParam(defaultValue = "false") boolean includeCompleted,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        boolean offers = !"requests".equalsIgnoreCase(type);
        String table = offers ? "offers" : "requests";
        String ownerCol = offers ? "offered_by_id" : "requested_by_id";
        String completedStatus = offers ? "GIVEN" : "FULFILLED";
        boolean hasGeo = lat != null && lon != null;
        boolean nearest = "nearest".equalsIgnoreCase(sort) && hasGeo;
        size = Math.max(1, Math.min(size, 50));
        page = Math.max(0, page);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("completedStatus", completedStatus)
                .addValue("limit", size)
                .addValue("offset", page * size);

        StringBuilder where = new StringBuilder(" WHERE u.blocked = false ");
        if (!includeCompleted) {
            where.append(" AND p.status <> :completedStatus ");
        }
        if (q != null && !q.isBlank()) {
            where.append(" AND (p.search_vector @@ websearch_to_tsquery('german', :q) " +
                    "OR p.title ILIKE :qLike OR p.description ILIKE :qLike) ");
            params.addValue("q", q.strip());
            params.addValue("qLike", "%" + q.strip() + "%");
        }
        if (category != null && !category.isBlank()) {
            where.append(" AND p.category = :category ");
            params.addValue("category", category);
        }
        if (withImage) {
            where.append(" AND p.image_url IS NOT NULL ");
        }
        String distanceSelect = "NULL::double precision AS distance_km";
        if (hasGeo) {
            params.addValue("lat", lat).addValue("lon", lon);
            distanceSelect = HAVERSINE + " AS distance_km";
            if (radiusKm != null && radiusKm > 0) {
                where.append(" AND p.lat IS NOT NULL AND ").append(HAVERSINE).append(" <= :radiusKm ");
                params.addValue("radiusKm", radiusKm);
            }
        }

        String orderBy = nearest
                ? " ORDER BY (p.lat IS NULL), distance_km ASC NULLS LAST, p.created_at DESC "
                : " ORDER BY p.created_at DESC ";

        String base = " FROM " + table + " p JOIN users u ON u.id = p." + ownerCol + where;

        Long total = jdbc.queryForObject("SELECT COUNT(*)" + base, params, Long.class);

        String sql = "SELECT p.id, p.title, p.description, p.region, p.category, p.quantity, " +
                "p.image_url, p.created_at, p.status, p.lat, p.lon, p.postal_code, p.city, " +
                "p." + ownerCol + " AS owner_id, u.username AS owner_username, " + distanceSelect +
                base + orderBy + " LIMIT :limit OFFSET :offset";

        List<Map<String, Object>> items = jdbc.query(sql, params, (rs, i) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", rs.getString("id"));
            m.put("title", rs.getString("title"));
            m.put("description", rs.getString("description"));
            m.put("region", rs.getString("region"));
            m.put("category", rs.getString("category"));
            m.put("quantity", rs.getInt("quantity"));
            m.put("imageUrl", rs.getString("image_url"));
            m.put("createdAt", DateTimeFormatter.ISO_INSTANT.format(rs.getTimestamp("created_at").toInstant()));
            m.put("status", rs.getString("status"));
            m.put("lat", rs.getObject("lat"));
            m.put("lon", rs.getObject("lon"));
            m.put("postalCode", rs.getString("postal_code"));
            m.put("city", rs.getString("city"));
            m.put(offers ? "offeredById" : "requestedById", rs.getString("owner_id"));
            m.put(offers ? "offeredByUsername" : "requestedByUsername", rs.getString("owner_username"));
            Object dist = rs.getObject("distance_km");
            m.put("distanceKm", dist == null ? null : Math.round(((Number) dist).doubleValue() * 10.0) / 10.0);
            return m;
        });

        return ResponseEntity.ok(Map.of(
                "items", items,
                "total", total == null ? 0 : total,
                "page", page,
                "size", size));
    }
}
