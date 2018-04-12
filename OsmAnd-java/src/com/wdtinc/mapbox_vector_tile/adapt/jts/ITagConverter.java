package com.wdtinc.mapbox_vector_tile.adapt.jts;

import net.osmand.binary.VectorTile;

import java.util.List;

/**
 * Process MVT tags and feature id, convert to user data object. The returned user data
 * object may be null.
 */
public interface ITagConverter {

    /**
     * Convert MVT user data to JTS user data object or null.
     *
     * @param id feature id, may be {@code null}
     * @param tags MVT feature tags, may be invalid
     * @param keysList layer key list
     * @param valuesList layer value list
     * @return user data object or null
     */
    Object toUserData(Long id,
                      List<Integer> tags,
                      List<String> keysList,
                      List<VectorTile.Tile.Value> valuesList);
}
