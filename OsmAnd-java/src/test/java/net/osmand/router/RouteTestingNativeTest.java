package net.osmand.router;

public class RouteTestingNativeTest extends RouteTestingTest {
    
    public RouteTestingNativeTest(String name, TestEntry te) {
        super(name, te);
    }
    
    @Override
    boolean isNative() {
        return true;
    }
}