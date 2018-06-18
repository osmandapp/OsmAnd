package com.wdtinc.mapbox_vector_tile.adapt.jts;

import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.CoordinateSequenceFilter;

/**
 * <p>Round each coordinate value to an integer.</p>
 *
 * <p>Mapbox vector tiles have fixed precision. This filter can be useful for reducing precision to
 * the extent of a MVT.</p>
 */
public final class RoundingFilter implements CoordinateSequenceFilter {

    public static final RoundingFilter INSTANCE = new RoundingFilter();

    private RoundingFilter() {}

    @Override
    public void filter(CoordinateSequence seq, int i) {
        seq.setOrdinate(i, 0, Math.round(seq.getOrdinate(i, 0)));
        seq.setOrdinate(i, 1, Math.round(seq.getOrdinate(i, 1)));
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean isGeometryChanged() {
        return true;
    }
}
