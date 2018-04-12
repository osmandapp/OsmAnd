package com.wdtinc.mapbox_vector_tile.adapt.jts;

import net.osmand.binary.VectorTile;

import java.util.List;

/**
 * Ignores tags, always returns null.
 *
 * @see ITagConverter
 */
public final class TagIgnoreConverter implements ITagConverter {
    @Override
    public Object toUserData(Long id, List<Integer> tags, List<String> keysList,
                             List<VectorTile.Tile.Value> valuesList) {
        return null;
    }
}
