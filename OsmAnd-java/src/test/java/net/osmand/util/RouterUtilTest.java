package net.osmand.util;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RouterUtilTest {
    
    private static final String ROAD_INFO_DELIMITER = ":";
    
    public static long getRoadId(String roadInfo) {
        if (roadInfo.contains(ROAD_INFO_DELIMITER)) {
            return Long.parseLong(roadInfo.split(ROAD_INFO_DELIMITER)[0]);
        }
        return Long.parseLong(roadInfo);
    }
    
    public static int getRoadStartPoint(String roadInfo) {
        if (roadInfo.contains(ROAD_INFO_DELIMITER)) {
            return Integer.parseInt(roadInfo.split(ROAD_INFO_DELIMITER)[1]);
        }
        return Integer.parseInt(roadInfo);
    }
    
    public static Set<Long> getExpectedIdSet(Map<String, String> expectedResults) {
        Set<Long> expectedSegments = new HashSet<>();
        for (String roadInfo : expectedResults.keySet()) {
            expectedSegments.add(RouterUtilTest.getRoadId(roadInfo));
        }
        return expectedSegments;
    }
    
    
}
