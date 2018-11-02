package net.osmand.router;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

public enum RoadSurface {
    PAVED("paved"),
    UNPAVED("unpaved"),
    ASPHALT("asphalt"),
    CONCRETE("concrete"),
    COMPACTED("compacted"),
    GRAVEL("gravel"),
    FINE_GRAVEL("fine_gravel"),
    PAVING_STONES("paving_stones"),
    SETT("sett"),
    COBBLESTONE("cobblestone"),
    PEBBLESTONE("pebblestone"),
    STONE("stone"),
    METAL("metal"),
    GROUND("ground", "mud"),
    WOOD("wood"),
    GRASS_PAVER("grass_paver"),
    GRASS("grass"),
    SAND("sand"),
    SALT("salt"),
    SNOW("snow"),
    ICE("ice"),
    CLAY("clay");

    final Set<String> surfaces = new TreeSet<>();

    RoadSurface(String... surfaces) {
        this.surfaces.addAll(Arrays.asList(surfaces));
    }

    boolean contains(String surface) {
        return surfaces.contains(surface);
    }
}
