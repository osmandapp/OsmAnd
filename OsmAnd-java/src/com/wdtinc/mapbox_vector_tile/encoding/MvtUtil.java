package com.wdtinc.mapbox_vector_tile.encoding;


import net.osmand.binary.VectorTile;

/**
 * <p>Useful misc operations for encoding 'Mapbox Vector Tiles'.</p>
 *
 * <p>See: <a href="https://github.com/mapbox/vector-tile-spec">https://github.com/mapbox/vector-tile-spec</a></p>
 */
public final class MvtUtil {

    /**
     * Return whether the MVT geometry type should be closed with a {@link GeomCmd#ClosePath}.
     *
     * @param geomType the type of MVT geometry
     * @return true if the geometry should be closed, false if it should not be closed
     */
    public static boolean shouldClosePath(VectorTile.Tile.GeomType geomType) {
        final boolean closeReq;

        switch(geomType) {
            case POLYGON:
                closeReq = true;
                break;
            default:
                closeReq = false;
                break;
        }

        return closeReq;
    }
}
