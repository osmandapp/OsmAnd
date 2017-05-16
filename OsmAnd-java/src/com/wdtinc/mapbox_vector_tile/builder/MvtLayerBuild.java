package com.wdtinc.mapbox_vector_tile.builder;

import com.wdtinc.mapbox_vector_tile.encoding.MvtValue;

import net.osmand.binary.VectorTile;

/**
 * Utility methods for building Mapbox-Vector-Tile layers.
 */
public final class MvtLayerBuild {

    /**
     * Create a new {@link com.wdtinc.mapbox_vector_tile.VectorTile.Tile.Layer.Builder} instance with
     * initialized version, name, and extent metadata.
     *
     * @param layerName name of the layer
     * @param mvtLayerParams tile creation parameters
     * @return new layer builder instance with initialized metadata.
     */
    public static VectorTile.Tile.Layer.Builder newLayerBuilder(String layerName, MvtLayerParams mvtLayerParams) {
        final VectorTile.Tile.Layer.Builder layerBuilder = VectorTile.Tile.Layer.newBuilder();
        layerBuilder.setVersion(2);
        layerBuilder.setName(layerName);
        layerBuilder.setExtent(mvtLayerParams.extent);

        return layerBuilder;
    }

    /**
     * Modifies {@code layerBuilder} to contain properties from {@code layerProps}.
     *
     * @param layerBuilder layer builder to write to
     * @param layerProps properties to write
     */
    public static void writeProps(VectorTile.Tile.Layer.Builder layerBuilder, MvtLayerProps layerProps) {

        // Add keys
        layerBuilder.addAllKeys(layerProps.getKeys());

        // Add values
        final Iterable<Object> vals = layerProps.getVals();
        for(Object o:vals){
            layerBuilder.addValues(MvtValue.toValue(o));
        }
    }
}
