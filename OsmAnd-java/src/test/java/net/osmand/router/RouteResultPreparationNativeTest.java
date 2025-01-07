package net.osmand.router;


public class RouteResultPreparationNativeTest extends RouteResultPreparationTest {
    
    public RouteResultPreparationNativeTest(String name, TestEntry te) {
        super(name, te);
    }
    
    @Override
    boolean isNative() {
        return true;
    }
}
