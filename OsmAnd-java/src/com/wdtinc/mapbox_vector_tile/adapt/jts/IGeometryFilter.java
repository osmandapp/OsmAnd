package com.wdtinc.mapbox_vector_tile.adapt.jts;

import com.vividsolutions.jts.geom.Geometry;

public interface IGeometryFilter {

    /**
     * Return true if the value should be accepted (pass), or false if the value should be rejected (fail).
     *
     * @param geometry input to test
     * @return true if the value should be accepted (pass), or false if the value should be rejected (fail)
     * @see Geometry
     */
    boolean accept(Geometry geometry);
}
