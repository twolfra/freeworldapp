package de.freeworldapp.app.geo;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlzGeoRepository extends JpaRepository<PlzGeo, String> {

    Optional<PlzGeo> findByPlz(String plz);

    @Query("SELECT p FROM PlzGeo p WHERE p.plz LIKE CONCAT(:prefix, '%') OR lower(p.city) LIKE CONCAT(lower(:prefix), '%') ORDER BY p.plz")
    List<PlzGeo> autocomplete(@Param("prefix") String prefix, Pageable pageable);
}
