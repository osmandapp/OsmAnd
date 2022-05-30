package net.osmand.router;

import net.osmand.data.LatLon;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by User on 07.03.2016.
 */
public class TestEntry {

    private String testName;
    private LatLon startPoint;
    private LatLon endPoint;
    private LatLon transitPoint1;
    private LatLon transitPoint2;
    private LatLon transitPoint3;
    private boolean ignore;
    private boolean ignoreNative;
    private Map<String, String> expectedResults;
    private Map<String, String> params;
    private int planRoadDirection;
    private boolean shortWay;

    
    public void setShortWay(boolean shortWay) {
		this.shortWay = shortWay;
	}
    
    public boolean isShortWay() {
    	return shortWay;
    }
    
    public List<LatLon> getTransitPoint() {
    	ArrayList<LatLon> arrayList = new ArrayList<>();
    	if(transitPoint1 != null) {
    		arrayList.add(transitPoint1);
    	}
    	if(transitPoint2 != null) {
    		arrayList.add(transitPoint2);
    	}
    	if(transitPoint3 != null) {
    		arrayList.add(transitPoint3);
    	}
    	return arrayList;
    }
    
    
    
    public int getPlanRoadDirection() {
		return planRoadDirection;
	}
    
    
    public void setPlanRoadDirection(int planRoadDirection) {
		this.planRoadDirection = planRoadDirection;
	}
    
    public LatLon getStartPoint() {
        return startPoint;
    }

    public void setStartPoint(LatLon startPoint) {
        this.startPoint = startPoint;
    }
    
    public void setIgnore(boolean ignore) {
		this.ignore = ignore;
	}
    
    public boolean isIgnore() {
    	return ignore;
    }
    
    public void setIgnoreNative(boolean ignoreNative) {
        this.ignoreNative = ignoreNative;
    }
    
    public boolean isIgnoreNative() {
        return ignoreNative;
    }

    public LatLon getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(LatLon endPoint) {
        this.endPoint = endPoint;
    }

    public Map<String, String> getExpectedResults() {
        return expectedResults;
    }

    public void setExpectedResults(Map<String, String> expectedResults) {
        this.expectedResults = expectedResults;
    }
    
    public void setParams(Map<String, String> params) {
		this.params = params;
	}
    
    public Map<String, String> getParams() {
		return params;
	}

    public String getTestName() {
        return testName;
    }

    public TestEntry(String testName, LatLon startPoint, LatLon endPoint, Map<String, String> expectedResults) {
        this.testName = testName;
        this.startPoint = startPoint;
        this.endPoint = endPoint;
        this.expectedResults = expectedResults;
    }
}
