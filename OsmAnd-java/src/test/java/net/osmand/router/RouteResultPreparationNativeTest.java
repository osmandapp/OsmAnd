package net.osmand.router;

import net.osmand.data.LatLon;

import java.util.Map;

public class RouteResultPreparationNativeTest extends RouteResultPreparationTest implements NativeLibraryTest {
    
    public RouteResultPreparationNativeTest(LatLon startPoint, LatLon endPoint, Map<String, String> expectedResults, Map<String, String> params) {
        super(startPoint, endPoint, expectedResults, params);
    }
    
    @Override
    boolean isNative() {
        return true;
    }
}
