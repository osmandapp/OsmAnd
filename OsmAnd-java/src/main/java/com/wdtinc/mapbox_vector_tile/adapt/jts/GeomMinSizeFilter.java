package com.wdtinc.mapbox_vector_tile.adapt.jts;

import com.vividsolutions.jts.geom.*;

/**
 * Filter {@link Polygon} and {@link MultiPolygon} by area or
 * {@link LineString} and {@link MultiLineString} by length.
 *
 * @see IGeometryFilter
 */
public final class GeomMinSizeFilter implements IGeometryFilter {

    /** Minimum area */
    private final double minArea;

    /** Minimum length */
    private final double minLength;

    /**
     * @param minArea minimum area required for a {@link Polygon} or {@link MultiPolygon}
     * @param minLength minimum length required for a {@link LineString} or {@link MultiLineString}
     */
    public GeomMinSizeFilter(double minArea, double minLength) {
        if(minArea < 0.0d) {
            throw new IllegalArgumentException("minArea must be >= 0");
        }
        if(minLength < 0.0d) {
            throw new IllegalArgumentException("minLength must be >= 0");
        }

        this.minArea = minArea;
        this.minLength = minLength;
    }

    @Override
    public boolean accept(Geometry geometry) {
        boolean accept = true;

        if((geometry instanceof Polygon || geometry instanceof MultiPolygon)
                && geometry.getArea() < minArea) {
            accept = false;

        } else if((geometry instanceof LineString || geometry instanceof MultiLineString)
                && geometry.getLength() < minLength) {
            accept = false;
        }

        return accept;
    }
}
