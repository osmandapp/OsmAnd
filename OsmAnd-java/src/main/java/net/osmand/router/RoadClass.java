package net.osmand.router;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

public enum RoadClass {
    MOTORWAY("motorway", "motorway_link"),
    STATE_ROAD("trunk", "trunk_link", "primary", "primary_link"),
    ROAD("secondary", "secondary_link", "tertiary", "tertiary_link", "unclassified"),
    STREET("residential", "living_street"),
    SERVICE("service"),
    TRACK("track", "road"),
    FOOTWAY("footway"),
    PATH("path"),
    CYCLE_WAY("cycleway");

    final Set<String> roadClasses = new TreeSet<>();

    RoadClass(String... classes) {
       roadClasses.addAll(Arrays.asList(classes));
    }

    boolean contains(String roadClass) {
        return roadClasses.contains(roadClass);
    }
}
