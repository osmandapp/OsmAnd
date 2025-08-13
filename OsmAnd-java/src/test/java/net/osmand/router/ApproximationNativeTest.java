package net.osmand.router;

public class ApproximationNativeTest extends ApproximationTest {
    
    public ApproximationNativeTest(String testName, ApproximationTest.ApproximationEntry te) {
        super(testName, te);
    }
    
    @Override
    boolean isNative() {
        return true;
    }
}
