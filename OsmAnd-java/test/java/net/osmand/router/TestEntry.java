package net.osmand.router;

import net.osmand.data.LatLon;

import java.util.Map;

/**
 * Created by User on 07.03.2016.
 */
public class TestEntry {

    private String testName;
    private LatLon startPoint;
    private LatLon endPoint;
    private Map<Long, String> expectedResults;

    public LatLon getStartPoint() {
        return startPoint;
    }

    public void setStartPoint(LatLon startPoint) {
        this.startPoint = startPoint;
    }

    public LatLon getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(LatLon endPoint) {
        this.endPoint = endPoint;
    }

    public Map<Long, String> getExpectedResults() {
        return expectedResults;
    }

    public void setExpectedResults(Map<Long, String> expectedResults) {
        this.expectedResults = expectedResults;
    }

    public String getTestName() {
        return testName;
    }

    public TestEntry(String testName, LatLon startPoint, LatLon endPoint, Map<Long, String> expectedResults) {
        this.testName = testName;
        this.startPoint = startPoint;
        this.endPoint = endPoint;
        this.expectedResults = expectedResults;
    }
}
