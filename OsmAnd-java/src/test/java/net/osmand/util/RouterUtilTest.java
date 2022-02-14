package net.osmand.util;

import java.io.File;
import java.nio.file.FileSystems;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
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
    
    public static String getNativeLibPath() {
        String nativeLibPath = FileSystems.getDefault().getPath("../../core-legacy/binaries").normalize().toAbsolutePath().toString();
        
        for (final File fileEntry : Objects.requireNonNull(new File(nativeLibPath).listFiles())) {
            if (fileEntry.isDirectory()) {
                File[] f = fileEntry.listFiles();
                for (final File f2 : Objects.requireNonNull(f)) {
                    if (f2.isDirectory()) {
                        File[] f3 = f2.listFiles();
                        for (File f4 : Objects.requireNonNull(f3)) {
                            if (f4.isDirectory() && f4.getName().equals("Release")
                                    || f4.isDirectory() && f4.getName().equals("Debug") ) {
                                return f4.getAbsolutePath();
                            }
                        }
                        return f2.getAbsolutePath();
                    }
                }
            }
        }
        return null;
    }
    
    
}
