package net.osmand.plus.routing.data;

import java.util.HashMap;
import java.util.Map;

public class StreetName {

    private final Map<String, String> names;

    public StreetName(Map<String, String> data) {
        this.names = data;
    }

    public StreetName() {
        names = new HashMap<>();
    }

    public Map<String, String> toMap() {
        return names;
    }
}