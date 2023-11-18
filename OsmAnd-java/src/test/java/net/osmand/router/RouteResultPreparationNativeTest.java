package net.osmand.router;


import org.junit.Ignore;

@Ignore
public class RouteResultPreparationNativeTest extends RouteResultPreparationTest {
    
    public RouteResultPreparationNativeTest(String name, TestEntry te) {
        super(name, te);
    }
    
    @Override
    boolean isNative() {
        return true;
    }
}
