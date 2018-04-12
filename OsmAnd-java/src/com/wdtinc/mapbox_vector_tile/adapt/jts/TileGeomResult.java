package com.wdtinc.mapbox_vector_tile.adapt.jts;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.wdtinc.mapbox_vector_tile.builder.MvtLayerParams;

import java.util.List;

/**
 * Processing result containing intersection geometry and MVT geometry.
 *
 * @see JtsAdapter#createTileGeom(Geometry, Envelope, GeometryFactory, MvtLayerParams, IGeometryFilter)
 */
public final class TileGeomResult {

    /** Intersection geometry (projection units and coordinates) */
    public final List<Geometry> intGeoms;

    /** Geometry in MVT coordinates (tile extent units, screen coordinates) */
    public final List<Geometry> mvtGeoms;

    /**
     * @param intGeoms geometry intersecting tile
     * @param mvtGeoms geometry for MVT
     * @throws NullPointerException if intGeoms or mvtGeoms are null
     */
    public TileGeomResult(List<Geometry> intGeoms, List<Geometry> mvtGeoms) {
        if(intGeoms == null || mvtGeoms == null) {
            throw new NullPointerException();
        }
        this.intGeoms = intGeoms;
        this.mvtGeoms = mvtGeoms;
    }
}
