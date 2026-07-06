package de.freeworldapp.app.geo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** German postal-code centroid (GeoNames, CC BY 4.0). Read-only reference data. */
@Entity
@Table(name = "plz_geo")
public class PlzGeo {

    @Id
    @Column(length = 10)
    private String plz;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false)
    private double lat;

    @Column(nullable = false)
    private double lon;

    public String getPlz() { return plz; }
    public String getCity() { return city; }
    public double getLat() { return lat; }
    public double getLon() { return lon; }
}
