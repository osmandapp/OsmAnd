package com.wdtinc.mapbox_vector_tile.encoding;


import net.osmand.binary.VectorTile;

/**
 * Utility class for working with {@link VectorTile.Tile.Value} instances.
 *
 * @see VectorTile.Tile.Value
 */
public final class MvtValue {

    /**
     * Covert an {@link Object} to a new {@link VectorTile.Tile.Value} instance.
     *
     * @param value target for conversion
     * @return new instance with String or primitive value set
     */
    public static VectorTile.Tile.Value toValue(Object value) {
        final VectorTile.Tile.Value.Builder tileValue = VectorTile.Tile.Value.newBuilder();

        if(value instanceof Boolean) {
            tileValue.setBoolValue((Boolean) value);

        } else if(value instanceof Integer) {
            tileValue.setSintValue((Integer) value);

        } else if(value instanceof Long) {
            tileValue.setSintValue((Long) value);

        } else if(value instanceof Float) {
            tileValue.setFloatValue((Float) value);

        } else if(value instanceof Double) {
            tileValue.setDoubleValue((Double) value);

        } else if(value instanceof String) {
            tileValue.setStringValue((String) value);
        }

        return tileValue.build();
    }

    /**
     * Convert {@link VectorTile.Tile.Value} to String or boxed primitive object.
     *
     * @param value target for conversion
     * @return String or boxed primitive
     */
    public static Object toObject(VectorTile.Tile.Value value) {
        Object result = null;

        if(value.hasDoubleValue()) {
            result = value.getDoubleValue();

        } else if(value.hasFloatValue()) {
            result = value.getFloatValue();

        } else if(value.hasIntValue()) {
            result = value.getIntValue();

        } else if(value.hasBoolValue()) {
            result = value.getBoolValue();

        } else if(value.hasStringValue()) {
            result = value.getStringValue();

        } else if(value.hasSintValue()) {
            result = value.getSintValue();

        } else if(value.hasUintValue()) {
            result = value.getUintValue();
        }

        return result;
    }

    /**
     * Check if {@code value} is valid for encoding as a MVT layer property value.
     *
     * @param value target to check
     * @return true is the object is a type that is supported by MVT
     */
    public static boolean isValidPropValue(Object value) {
        boolean isValid = false;

        if(value instanceof Boolean || value instanceof Integer || value instanceof Long
                || value instanceof Float || value instanceof Double || value instanceof String) {
            isValid = true;
        }

        return isValid;
    }
}
