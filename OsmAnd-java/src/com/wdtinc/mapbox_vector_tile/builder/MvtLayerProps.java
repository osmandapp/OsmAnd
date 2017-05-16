package com.wdtinc.mapbox_vector_tile.builder;

import com.wdtinc.mapbox_vector_tile.encoding.MvtValue;

import java.util.*;

/**
 * Support MVT features that must reference properties by their key and value index.
 */
public final class MvtLayerProps {
    private LinkedHashMap<String, Integer> keys;
    private LinkedHashMap<Object, Integer> vals;

    public MvtLayerProps() {
        keys = new LinkedHashMap<>();
        vals = new LinkedHashMap<>();
    }

    public Integer keyIndex(String k) {
        return keys.get(k);
    }

    public Integer valueIndex(Object v) {
        return vals.get(v);
    }

    /**
     * Add the key and return it's index code. If the key already is present, the previous
     * index code is returned and no insertion is done.
     *
     * @param key key to add
     * @return index of the key
     */
    public int addKey(String key) {
        Objects.requireNonNull(key);
        int nextIndex = keys.size();
        final Integer mapIndex = keys.putIfAbsent(key, nextIndex);
        return mapIndex == null ? nextIndex : mapIndex;
    }

    /**
     * Add the value and return it's index code. If the value already is present, the previous
     * index code is returned and no insertion is done. If {@code value} is an unsupported type
     * for encoding in a MVT, then it will not be added.
     *
     * @param value value to add
     * @return index of the value, -1 on unsupported value types
     * @see MvtValue#isValidPropValue(Object)
     */
    public int addValue(Object value) {
        Objects.requireNonNull(value);
        if(!MvtValue.isValidPropValue(value)) {
            return -1;
        }

        int nextIndex = vals.size();
        final Integer mapIndex = vals.putIfAbsent(value, nextIndex);
        return mapIndex == null ? nextIndex : mapIndex;
    }

    public Iterable<String> getKeys() {
        return keys.keySet();
    }

    public Iterable<Object> getVals() {
        return vals.keySet();
    }
}
